package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.DestroyImageParameters;
import org.ovirt.engine.core.common.action.MergeParameters;
import org.ovirt.engine.core.common.action.MergeStatusReturnValue;
import org.ovirt.engine.core.common.action.RemoveSnapshotSingleDiskLiveStep;
import org.ovirt.engine.core.common.action.RemoveSnapshotSingleDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.VmBlockJobType;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.Image;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InternalCommandAttribute
public class RemoveSnapshotSingleDiskLiveCommand<T extends RemoveSnapshotSingleDiskParameters>
        extends RemoveSnapshotSingleDiskCommandBase<T> {
    private static final Logger log = LoggerFactory.getLogger(RemoveSnapshotSingleDiskLiveCommand.class);

    public RemoveSnapshotSingleDiskLiveCommand(T parameters) {
        super(parameters);
    }

    public RemoveSnapshotSingleDiskLiveCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeCommand() {
        // Let doPolling() drive the execution; we don't have any guarantee that
        // executeCommand() will finish before doPolling() is called, and we don't
        // want to possibly run the first command twice.
        setSucceeded(true); // Allow runAction to succeed
    }

    public void proceedCommandExecution() {
        // Steps are executed such that:
        //  a) all logic before the command runs is idempotent
        //  b) the command is the last action in the step
        // This allows for recovery after a crash at any point during command execution.

        log.debug("Proceeding with execution of RemoveSnapshotSingleDiskLiveCommand");
        if (getParameters().getCommandStep() == null) {
            getParameters().setCommandStep(getInitialMergeStepForImage(getParameters().getImageId()));
            getParameters().setChildCommands(new HashMap<RemoveSnapshotSingleDiskLiveStep, Guid>());
        }

        // Upon recovery or after invoking a new child command, our map may be missing an entry
        syncChildCommandList();
        Guid currentChildId = getCurrentChildId();

        VdcReturnValueBase vdcReturnValue = null;
        if (currentChildId != null) {
            switch (CommandCoordinatorUtil.getCommandStatus(currentChildId)) {
            case ACTIVE:
            case NOT_STARTED:
                log.info("Waiting on Live Merge command step '{}' to complete",
                        getParameters().getCommandStep());
                return;

            case SUCCEEDED:
                CommandEntity cmdEntity = CommandCoordinatorUtil.getCommandEntity(currentChildId);
                if (!cmdEntity.isCallbackNotified()) {
                    log.info("Waiting on Live Merge command step '{}' to finalize",
                            getParameters().getCommandStep());
                    return;
                }

                vdcReturnValue = CommandCoordinatorUtil.getCommandReturnValue(currentChildId);
                if (vdcReturnValue != null && vdcReturnValue.getSucceeded()) {
                    log.debug("Child command '{}' succeeded",
                            getParameters().getCommandStep());
                    getParameters().setCommandStep(getParameters().getNextCommandStep());
                    break;
                } else {
                    log.error("Child command '{}' failed: {}",
                            getParameters().getCommandStep(),
                            (vdcReturnValue != null
                                    ? vdcReturnValue.getExecuteFailedMessages()
                                    : "null return value")
                    );
                    setCommandStatus(CommandStatus.FAILED);
                    return;
                }

            case FAILED:
            case FAILED_RESTARTED:
                log.error("Failed child command status for step '{}'",
                        getParameters().getCommandStep());
                setCommandStatus(CommandStatus.FAILED);
                return;

            case UNKNOWN:
                log.error("Unknown child command status for step '{}'",
                        getParameters().getCommandStep());
                setCommandStatus(CommandStatus.FAILED);
                return;
            }
        }

        log.info("Executing Live Merge command step '{}'", getParameters().getCommandStep());

        Pair<VdcActionType, ? extends VdcActionParametersBase> nextCommand = null;
        switch (getParameters().getCommandStep()) {
        case MERGE:
            nextCommand = new Pair<>(VdcActionType.Merge, buildMergeParameters());
            getParameters().setNextCommandStep(RemoveSnapshotSingleDiskLiveStep.MERGE_STATUS);
            break;
        case MERGE_STATUS:
            getParameters().setMergeCommandComplete(true);
            nextCommand = new Pair<>(VdcActionType.MergeStatus, buildMergeParameters());
            getParameters().setNextCommandStep(RemoveSnapshotSingleDiskLiveStep.DESTROY_IMAGE);
            break;
        case DESTROY_IMAGE:
            if (vdcReturnValue != null) {
                getParameters().setMergeStatusReturnValue((MergeStatusReturnValue) vdcReturnValue.getActionReturnValue());
            } else if (getParameters().getMergeStatusReturnValue() == null) {
                // If the images were already merged, just add the orphaned image
                getParameters().setMergeStatusReturnValue(synthesizeMergeStatusReturnValue());
            }
            nextCommand = new Pair<>(VdcActionType.DestroyImage, buildDestroyImageParameters());
            getParameters().setNextCommandStep(RemoveSnapshotSingleDiskLiveStep.COMPLETE);
            break;
        case COMPLETE:
            getParameters().setDestroyImageCommandComplete(true);
            setCommandStatus(CommandStatus.SUCCEEDED);
            break;
        }

        persistCommand(getParameters().getParentCommand(), true);
        if (nextCommand != null) {
            CommandCoordinatorUtil.executeAsyncCommand(nextCommand.getFirst(), nextCommand.getSecond(), cloneContextAndDetachFromParent());
            // Add the child, but wait, it's a race!  child will start, task may spawn, get polled, and we won't have the child id
        }
    }

    /**
     * Updates (but does not persist) the parameters.childCommands list to ensure the current
     * child command is present.  This is necessary in various entry points called externally
     * (e.g. by endAction()), which can be called after a child command is started but before
     * the main proceedCommandExecution() loop has persisted the updated child list.
     */
    private void syncChildCommandList() {
        List<Guid> childCommandIds = CommandCoordinatorUtil.getChildCommandIds(getCommandId());
        if (childCommandIds.size() != getParameters().getChildCommands().size()) {
            for (Guid id : childCommandIds) {
                if (!getParameters().getChildCommands().containsValue(id)) {
                    getParameters().getChildCommands().put(getParameters().getCommandStep(), id);
                    break;
                }
            }
        }
    }

    private Guid getCurrentChildId() {
        return getParameters().getChildCommands().get(getParameters().getCommandStep());
    }

    private RemoveSnapshotSingleDiskLiveStep getInitialMergeStepForImage(Guid imageId) {
        Image image = getImageDao().get(imageId);
        if (image.getStatus() == ImageStatus.ILLEGAL
                && (image.getParentId().equals(Guid.Empty))) {
            List<DiskImage> children = DbFacade.getInstance().getDiskImageDao()
                    .getAllSnapshotsForParent(imageId);
            if (children.isEmpty()) {
                // An illegal, orphaned image means its contents have been merged
                log.info("Image has been previously merged, proceeding with deletion");
                return RemoveSnapshotSingleDiskLiveStep.DESTROY_IMAGE;
            }
        }
        return RemoveSnapshotSingleDiskLiveStep.MERGE;
    }

    private boolean completedMerge() {
        return getParameters().getCommandStep() == RemoveSnapshotSingleDiskLiveStep.DESTROY_IMAGE
                || getParameters().getCommandStep() == RemoveSnapshotSingleDiskLiveStep.COMPLETE;
    }

    private MergeParameters buildMergeParameters() {
        MergeParameters parameters = new MergeParameters(
                getVdsId(),
                getVmId(),
                getActiveDiskImage(),
                getDiskImage(),
                getDestinationDiskImage(),
                0);
        parameters.setParentCommand(VdcActionType.RemoveSnapshotSingleDiskLive);
        parameters.setParentParameters(getParameters());
        return parameters;
    }

    private DestroyImageParameters buildDestroyImageParameters() {
        DestroyImageParameters parameters = new DestroyImageParameters(
                getVdsId(),
                getVmId(),
                getDiskImage().getStoragePoolId(),
                getDiskImage().getStorageIds().get(0),
                getActiveDiskImage().getId(),
                new ArrayList<>(getParameters().getMergeStatusReturnValue().getImagesToRemove()),
                getDiskImage().isWipeAfterDelete(),
                false);
        parameters.setParentCommand(VdcActionType.RemoveSnapshotSingleDiskLive);
        parameters.setParentParameters(getParameters());
        return parameters;
    }

    private DiskImage getActiveDiskImage() {
        Guid snapshotId = getSnapshotDao().getId(getVmId(), Snapshot.SnapshotType.ACTIVE);
        return getDiskImageDao().getDiskSnapshotForVmSnapshot(getDiskImage().getId(), snapshotId);
    }

    /**
     * Add orphaned, already-merged images from this snapshot to a MergeStatusReturnValue that
     * can be used by the DESTROY_IMAGE command step to tell what needs to be deleted.
     *
     * @return A suitable MergeStatusReturnValue object
     */
    private MergeStatusReturnValue synthesizeMergeStatusReturnValue() {
        Set<Guid> images = new HashSet<>();
        images.add(getDiskImage().getImageId());
        return new MergeStatusReturnValue(VmBlockJobType.UNKNOWN, images);
    }

    public void onSucceeded() {
        syncDbRecords(true);
        endSuccessfully();
        log.info("Successfully merged snapshot '{}' images '{}'..'{}'",
                getDiskImage().getImage().getSnapshotId(),
                getDiskImage().getImageId(),
                getDestinationDiskImage() != null ? getDestinationDiskImage().getImageId() : "(n/a)");
    }

    private void syncDbRecordsMergeFailure() {
        DiskImage curr = getDestinationDiskImage();
        while (!curr.getImageId().equals(getDiskImage().getImageId())) {
            curr = getDbFacade().getDiskImageDao().getSnapshotById(curr.getParentId());
            getImageDao().updateStatus(curr.getImageId(), ImageStatus.ILLEGAL);
        }
    }

    /**
     * After merging the snapshots, update the image and snapshot records in the
     * database to reflect the changes.  This handles either forward or backwards
     * merge (detected).  It will either then remove the images, or mark them
     * illegal (to handle the case where image deletion failed).
     *
     * @param removeImages Remove the images from the database, or if false, only
     *                     mark them illegal
     */
    private void syncDbRecords(boolean removeImages) {
        // If deletion failed after a backwards merge, the snapshots' images need to be swapped
        // as they would upon success.  Instead of removing them, mark them illegal.
        DiskImage baseImage = getDiskImage();
        DiskImage topImage = getDestinationDiskImage();

        // The vdsm merge verb may decide to perform a forward or backward merge.
        if (topImage == null) {
            log.debug("No merge destination image, not updating image/snapshot association");
        } else if (getParameters().getMergeStatusReturnValue().getBlockJobType() == VmBlockJobType.PULL) {
            // For forward merge, the volume format and type may change.
            topImage.setvolumeFormat(baseImage.getVolumeFormat());
            topImage.setVolumeType(baseImage.getVolumeType());
            topImage.setParentId(baseImage.getParentId());
            topImage.setImageStatus(ImageStatus.OK);

            getBaseDiskDao().update(topImage);
            getImageDao().update(topImage.getImage());
            updateDiskImageDynamic(topImage);

            updateVmConfigurationForImageRemoval(baseImage.getImage().getSnapshotId(),
                    baseImage.getImageId());
        } else {
            // For backwards merge, the prior base image now has the data associated with the newer
            // snapshot we want to keep.  Re-associate this older image with the newer snapshot.
            // The base snapshot is deleted if everything went well.  In case it's not deleted, we
            // hijack it to preserve a link to the broken image.  This makes the image discoverable
            // so that we can retry the deletion later, yet doesn't corrupt the VM image chain.
            List<DiskImage> children = DbFacade.getInstance().getDiskImageDao()
                    .getAllSnapshotsForParent(topImage.getImageId());
            if (!children.isEmpty()) {
                DiskImage childImage = children.get(0);
                childImage.setParentId(baseImage.getImageId());
                getImageDao().update(childImage.getImage());
            }

            Image oldTopImage = topImage.getImage();
            topImage.setImage(baseImage.getImage());
            baseImage.setImage(oldTopImage);

            Guid oldTopSnapshotId = topImage.getImage().getSnapshotId();
            topImage.getImage().setSnapshotId(baseImage.getImage().getSnapshotId());
            baseImage.getImage().setSnapshotId(oldTopSnapshotId);

            boolean oldTopIsActive = topImage.getImage().isActive();
            topImage.getImage().setActive(baseImage.getImage().isActive());
            baseImage.getImage().setActive(oldTopIsActive);

            topImage.setImageStatus(ImageStatus.OK);
            getBaseDiskDao().update(topImage);
            getImageDao().update(topImage.getImage());
            updateDiskImageDynamic(topImage);

            getBaseDiskDao().update(baseImage);
            getImageDao().update(baseImage.getImage());

            updateVmConfigurationForImageChange(topImage.getImage().getSnapshotId(),
                    baseImage.getImageId(), topImage);
            updateVmConfigurationForImageRemoval(baseImage.getImage().getSnapshotId(),
                    topImage.getImageId());
        }

        Set<Guid> imagesToUpdate = getParameters().getMergeStatusReturnValue().getImagesToRemove();
        if (imagesToUpdate == null) {
            log.error("Failed to update orphaned images in db: image list could not be retrieved");
            return;
        }
        for (Guid imageId : imagesToUpdate) {
            if (removeImages) {
                getImageDao().remove(imageId);
            } else {
                // The (illegal && no-parent && no-children) status indicates an orphaned image.
                Image image = getImageDao().get(imageId);
                image.setStatus(ImageStatus.ILLEGAL);
                image.setParentId(Guid.Empty);
                getImageDao().update(image);
            }
        }
    }

    private void updateVmConfigurationForImageChange(final Guid snapshotId, final Guid oldImageId, final DiskImage newImage) {
        try {
            lockVmSnapshotsWithWait(getVm());

            TransactionSupport.executeInNewTransaction(
                    new TransactionMethod<Object>() {
                        @Override
                        public Object runInTransaction() {
                            Snapshot s = getSnapshotDao().get(snapshotId);
                            s = ImagesHandler.prepareSnapshotConfigWithAlternateImage(s, oldImageId, newImage);
                            getSnapshotDao().update(s);
                            return null;
                        }
                    });
        } finally {
            if (getSnapshotsEngineLock() != null) {
                getLockManager().releaseLock(getSnapshotsEngineLock());
            }
        }
    }

    private void updateVmConfigurationForImageRemoval(final Guid snapshotId, final Guid imageId) {
        try {
            lockVmSnapshotsWithWait(getVm());

            TransactionSupport.executeInNewTransaction(
                    new TransactionMethod<Object>() {
                        @Override
                        public Object runInTransaction() {
                            Snapshot s = getSnapshotDao().get(snapshotId);
                            s = ImagesHandler.prepareSnapshotConfigWithoutImageSingleImage(s, imageId);
                            getSnapshotDao().update(s);
                            return null;
                        }
                    });
        } finally {
            if (getSnapshotsEngineLock() != null) {
                getLockManager().releaseLock(getSnapshotsEngineLock());
            }
        }
    }

    @Override
    protected void endSuccessfully() {
        // SPM tasks report their final status to the spawning command's parent (in this case, us)
        // rather than the spawning command (ie DestroyImageCommand); therefore, we must redirect
        // endAction so that upon SPM task completion the status is propagated to the proper command.
        if (getParameters().getCommandStep() == RemoveSnapshotSingleDiskLiveStep.DESTROY_IMAGE) {
            syncChildCommandList();
            Guid currentChildId = getCurrentChildId();
            if (!Guid.isNullOrEmpty(currentChildId)) {
                CommandBase<?> command = CommandCoordinatorUtil.retrieveCommand(currentChildId);
                if (command != null) {
                    Backend.getInstance().endAction(VdcActionType.DestroyImage,
                            command.getParameters(),
                            cloneContextAndDetachFromParent());
                    CommandCoordinatorUtil.getCommandEntity(currentChildId).setCallbackNotified(true);
                }
            }
        }
        setSucceeded(true);
    }

    public void onFailed() {
        if (!completedMerge()) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    syncDbRecordsMergeFailure();
                    return null;
                }
            });
            log.error("Merging of snapshot '{}' images '{}'..'{}' failed. Images have been" +
                            " marked illegal and can no longer be previewed or reverted to." +
                            " Please retry Live Merge on the snapshot to complete the operation.",
                    getDiskImage().getImage().getSnapshotId(),
                    getDiskImage().getImageId(),
                    getDestinationDiskImage() != null ? getDestinationDiskImage().getImageId() : "(n/a)"
            );

        } else {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    syncDbRecords(false);
                    return null;
                }
            });
            log.error("Snapshot '{}' images '{}'..'{}' merged, but volume removal failed." +
                            " Some or all of the following volumes may be orphaned: {}." +
                            " Please retry Live Merge on the snapshot to complete the operation.",
                    getDiskImage().getImage().getSnapshotId(),
                    getDiskImage().getImageId(),
                    getDestinationDiskImage() != null ? getDestinationDiskImage().getImageId() : "(n/a)",
                    getParameters().getMergeStatusReturnValue().getImagesToRemove());
        }
        endWithFailure();
    }

    @Override
    protected void endWithFailure() {
        // See comment in endSuccessfully() for an explanation of this redirection
        if (getParameters().getCommandStep() == RemoveSnapshotSingleDiskLiveStep.DESTROY_IMAGE) {
            syncChildCommandList();
            Guid currentChildId = getCurrentChildId();
            if (!Guid.isNullOrEmpty(currentChildId)) {
                CommandBase<?> command = CommandCoordinatorUtil.retrieveCommand(currentChildId);
                if (command != null) {
                    command.getParameters().setTaskGroupSuccess(false);
                    Backend.getInstance().endAction(VdcActionType.DestroyImage,
                            command.getParameters(),
                            cloneContextAndDetachFromParent());
                }
            }
        }
        setSucceeded(true);
    }

    @Override
    public CommandCallback getCallback() {
        return new RemoveSnapshotSingleDiskLiveCommandCallback();
    }
}
