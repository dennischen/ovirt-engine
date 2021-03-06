package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.hostdev.HostDeviceManager;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.network.host.HostNicVfsConfigHelper;
import org.ovirt.engine.core.bll.quota.QuotaManager;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.action.ProcessDownVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.action.VmPoolSimpleUserParameters;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.utils.VmDeviceCommonUtils;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmPoolDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class ProcessDownVmCommand<T extends ProcessDownVmParameters> extends CommandBase<T> {

    private static final Logger log = LoggerFactory.getLogger(ProcessDownVmCommand.class);

    @Inject
    private HostDeviceManager hostDeviceManager;

    @Inject
    private HostNicVfsConfigHelper hostNicVfsConfigHelper;

    protected ProcessDownVmCommand(Guid commandId) {
        super(commandId);
    }

    public ProcessDownVmCommand(T parameters) {
        this(parameters, null);
    }

    public ProcessDownVmCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        setVmId(getParameters().getId());
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        boolean removedStatelessSnapshot = detachUsers();
        if (!removedStatelessSnapshot) {
            // If we are dealing with a prestarted Vm or a regular Vm - clean stateless images
            // Otherwise this was already done in DetachUserFromVmFromPoolCommand
            removeVmStatelessImages();
        }

        QuotaManager.getInstance().rollbackQuotaByVmId(getVmId());
        removeStatelessVmUnmanagedDevices();

        boolean vmHasDirectPassthroughDevices = releaseUsedHostDevices();

        Guid hostId = cleanupVfs();
        refreshHostIfNeeded(hostId == null ? (vmHasDirectPassthroughDevices ? getVm().getDedicatedVmForVds() : null)
                : hostId);

        applyNextRunConfiguration();
    }

    private boolean releaseUsedHostDevices() {
        if (hostDeviceManager.checkVmNeedsDirectPassthrough(getVm())) {
            try {
                hostDeviceManager.acquireHostDevicesLock(getVm().getDedicatedVmForVds());
                hostDeviceManager.freeVmHostDevices(getVmId());
            } finally {
                hostDeviceManager.releaseHostDevicesLock(getVm().getDedicatedVmForVds());
            }
            return true;
        }

        return false;
    }

    private Guid cleanupVfs() {
        Guid hostId = hostNicVfsConfigHelper.removeVmIdFromVfs(getVmId());
        return hostId;
    }

    private void refreshHostIfNeeded(Guid hostId) {
        // refresh host to get the host devices that were detached from the VM and re-attached to the host
        if (!getParameters().isSkipHostRefresh() && hostId != null) {
            runInternalAction(VdcActionType.RefreshHost, new VdsActionParameters(hostId));
        }
    }

    private boolean detachUsers() {
        // check if this is a VM from a VM pool
        if (getVm().getVmPoolId() == null) {
            return false;
        }

        List<DbUser> users = getDbUserDAO().getAllForVm(getVmId());
        // check if this VM is attached to a user
        if (users == null || users.isEmpty()) {
            // if not, check if new version or need to restore stateless
            runInternalActionWithTasksContext(VdcActionType.RestoreStatelessVm,
                    new VmOperationParameterBase(getVmId()),
                    getLock());
            return true;
        }
        VmPool pool = getVmPoolDAO().get(getVm().getVmPoolId());
        if (pool != null && pool.getVmPoolType() == VmPoolType.Automatic) {
            // should be only one user in the collection
            for (DbUser dbUser : users) {
                runInternalActionWithTasksContext(VdcActionType.DetachUserFromVmFromPool,
                        new VmPoolSimpleUserParameters(getVm().getVmPoolId(), dbUser.getId(), getVmId()), getLock());
            }

            return true;
        }

        return false;
    }

    private VmPoolDAO getVmPoolDAO() {
        return DbFacade.getInstance().getVmPoolDao();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.emptyList();
    }

    /**
     * Remove VM's unmanaged devices that are created during run-once or stateless run.
     */
    private void removeStatelessVmUnmanagedDevices() {
        if (getVm().isStateless() || isRunOnce()) {
            final List<VmDevice> vmDevices =
                    DbFacade.getInstance()
                            .getVmDeviceDao()
                            .getUnmanagedDevicesByVmId(getVmId());

            for (VmDevice device : vmDevices) {
                // do not remove device if appears in white list
                if (!VmDeviceCommonUtils.isInWhiteList(device.getType(), device.getDevice())) {
                    DbFacade.getInstance().getVmDeviceDao().remove(device.getId());
                }
            }
        }
    }

    /**
     * This method checks if we are stopping a VM that was started by run-once In such case we will may have 2 devices,
     * one managed and one unmanaged for CD or Floppy This is not supported currently by libvirt that allows only one
     * CD/Floppy This code should be removed if libvirt will support in future multiple CD/Floppy
     */
    private boolean isRunOnce() {
        List<VmDevice> cdList =
                DbFacade.getInstance()
                        .getVmDeviceDao()
                        .getVmDeviceByVmIdTypeAndDevice(getVmId(),
                                VmDeviceGeneralType.DISK,
                                VmDeviceType.CDROM.getName());
        List<VmDevice> floppyList =
                DbFacade.getInstance()
                        .getVmDeviceDao()
                        .getVmDeviceByVmIdTypeAndDevice(getVmId(),
                                VmDeviceGeneralType.DISK,
                                VmDeviceType.FLOPPY.getName());

        return (cdList.size() > 1 || floppyList.size() > 1);
    }

    /**
     * Update VM configuration with NEXT_RUN configuration, if exists.
     */
    private void applyNextRunConfiguration() {
        // Remove snpashot first, in case other update is in progress, it will block this one with exclusive lock
        // and any newer update should be preffered to this one.
        Snapshot runSnap = getSnapshotDAO().get(getVmId(), SnapshotType.NEXT_RUN);
        if (runSnap != null) {
            getSnapshotDAO().remove(runSnap.getId());
            Date originalCreationDate = getVm().getVmCreationDate();
            new SnapshotsManager().updateVmFromConfiguration(getVm(), runSnap.getVmConfiguration());
            // override creation date because the value in the config is the creation date of the config, not the vm
            getVm().setVmCreationDate(originalCreationDate);

            runInternalAction(VdcActionType.UpdateVm, createUpdateVmParameters());
        }
    }

    private VmManagementParametersBase createUpdateVmParameters() {
        // clear non updateable fields got from config
        getVm().setExportDate(null);
        getVm().setOvfVersion(null);

        VmManagementParametersBase updateVmParams = new VmManagementParametersBase(getVm());
        updateVmParams.setUpdateWatchdog(true);
        updateVmParams.setSoundDeviceEnabled(false);
        updateVmParams.setBalloonEnabled(false);
        updateVmParams.setVirtioScsiEnabled(false);
        updateVmParams.setClearPayload(true);
        updateVmParams.setUpdateRngDevice(true);
        for (GraphicsType graphicsType : GraphicsType.values()) {
            updateVmParams.getGraphicsDevices().put(graphicsType, null);
        }

        for (VmDevice device : getVm().getManagedVmDeviceMap().values()) {
            switch (device.getType()) {
                case WATCHDOG:
                    updateVmParams.setWatchdog(new VmWatchdog(device));
                    break;
                case SOUND:
                    updateVmParams.setSoundDeviceEnabled(true);
                    break;
                case BALLOON:
                    updateVmParams.setBalloonEnabled(true);
                    break;
                case CONTROLLER:
                    if (VmDeviceType.VIRTIOSCSI.getName().equals(device.getDevice())) {
                        updateVmParams.setVirtioScsiEnabled(true);
                    }
                    break;
                case DISK:
                    if (VmPayload.isPayload(device.getSpecParams())) {
                        updateVmParams.setVmPayload(new VmPayload(device));
                    }
                    break;
                case CONSOLE:
                    updateVmParams.setConsoleEnabled(true);
                    break;
                case RNG:
                    updateVmParams.setRngDevice(new VmRngDevice(device));
                    break;
                case GRAPHICS:
                    updateVmParams.getGraphicsDevices().put(GraphicsType.fromString(device.getDevice()),
                            new GraphicsDevice(device));
                    break;
                default:
            }
        }

        // clear these fields as these are non updatable
        getVm().getManagedVmDeviceMap().clear();
        getVm().getVmUnamagedDeviceList().clear();

        return updateVmParams;
    }

    private void removeVmStatelessImages() {
        if (getSnapshotDAO().exists(getVmId(), SnapshotType.STATELESS)) {
            log.info("Deleting snapshot for stateless vm '{}'", getVmId());
            runInternalAction(VdcActionType.RestoreStatelessVm,
                    new VmOperationParameterBase(getVmId()),
                    ExecutionHandler.createDefaultContextForTasks(getContext(), getLock()));
        }
    }
}
