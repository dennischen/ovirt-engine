package org.ovirt.engine.core.bll.hostdev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.HostDeviceDao;
import org.ovirt.engine.core.dao.VdsDynamicDAO;
import org.ovirt.engine.core.dao.VmDeviceDAO;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManager;

@ApplicationScoped
public class HostDeviceManager {

    @Inject
    private VdsDynamicDAO hostDynamicDao;

    @Inject
    private LockManager lockManager;

    @Inject
    private HostDeviceDao hostDeviceDao;

    @Inject
    private VmDeviceDAO vmDeviceDao;

    @Inject
    private BackendInternal backend;

    public void init() {
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<>();
        // It is sufficient to refresh only the devices of 'UP' hosts since other hosts
        // will have their devices refreshed in InitVdsOnUpCommand
        for (Guid hostId : hostDynamicDao.getIdsOfHostsWithStatus(VDSStatus.Up)) {
            parameters.add(new VdsActionParameters(hostId));
        }

        backend.runInternalMultipleActions(VdcActionType.RefreshHostDevices, parameters);
        hostDeviceDao.cleanDownVms();
    }

    /**
     * Checks whether the specified VM is pinned to a host and has host devices directly attached to it
     *
     * @param vm
     * @return true if the specified VM is pinned to a host and has host devices directly attached to it
     */
    public boolean checkVmNeedsDirectPassthrough(VM vm) {
        return vm.getDedicatedVmForVds() != null && supportsHostDevicePassthrough(vm) &&
                checkVmNeedsDirectPassthrough(vm.getId());
    }

    private boolean checkVmNeedsDirectPassthrough(Guid vmId) {
        return vmDeviceDao.existsVmDeviceByVmIdAndType(vmId, VmDeviceGeneralType.HOSTDEV);
    }

    private boolean checkVmNeedsHostDevices(Guid vmId) {
        List<VmDevice> vfs = vmDeviceDao.getVmDeviceByVmIdTypeAndDevice(vmId,
                VmDeviceGeneralType.INTERFACE,
                VmDeviceType.HOST_DEVICE.getName());

        return !vfs.isEmpty() || checkVmNeedsDirectPassthrough(vmId);
    }

    public boolean checkVmHostDeviceAvailability(VM vm, Guid vdsId) {
        // if vm's cluster doesn't support hostdev, it's requirements for host devices are trivially fulfilled
        return !supportsHostDevicePassthrough(vm) || hostDeviceDao.checkVmHostDeviceAvailability(vm.getId(), vdsId);
    }

    public void allocateVmHostDevices(Guid vmId) {
        hostDeviceDao.markHostDevicesUsedByVmId(vmId);
    }

    public void freeVmHostDevices(Guid vmId) {
        hostDeviceDao.freeHostDevicesUsedByVmId(vmId);
    }

    public void acquireHostDevicesLock(Guid vdsId) {
        lockManager.acquireLockWait(new EngineLock(getExclusiveLockForHostDevices(vdsId)));
    }

    public void releaseHostDevicesLock(Guid vdsId) {
        lockManager.releaseLock(new EngineLock(getExclusiveLockForHostDevices(vdsId)));
    }

    private static Map<String, Pair<String, String>> getExclusiveLockForHostDevices(Guid vdsId) {
        return Collections.singletonMap(
                vdsId.toString(),
                LockMessagesMatchUtil.makeLockingPair(
                        LockingGroup.HOST_DEVICES,
                        VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
    }

    private static boolean supportsHostDevicePassthrough(VM vm) {
        return FeatureSupported.hostDevicePassthrough(vm.getVdsGroupCompatibilityVersion());
    }

    /**
     * Calls <code>VdcActionType.RefreshHost</code> on the specified host, in case any of the specified vms contain
     * host devices (that were attached directly or via the SRIOV scheduling)
     *
     * @param vmIds
     * @param hostId
     */
    public void refreshHostIfAnyVmHasHostDevices(Collection<Guid> vmIds, Guid hostId) {
        for (Guid vmId : vmIds) {
            if (checkVmNeedsHostDevices(vmId)) {
                backend.runInternalAction(VdcActionType.RefreshHost, new VdsActionParameters(hostId));
                return;
            }
        }
    }
}
