package org.ovirt.engine.core.bll.scheduling.policyunits;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.dao.VdsGroupDAO;
import org.ovirt.engine.core.dao.VmDAO;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class CpuAndMemoryBalancingPolicyUnitTest extends AbstractPolicyUnitTest {
    protected  <T extends CpuAndMemoryBalancingPolicyUnit> T mockUnit(
            Class<T> unitType,
            VDSGroup cluster,
            Map<Guid, VDS> hosts,
            Map<Guid, VM> vms)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException, ParseException {
        T unit = spy(unitType.getConstructor(PolicyUnit.class).newInstance((PolicyUnit)null));

        // mock current time
        doReturn(TIME_FORMAT.parse("2015-01-01 12:00:00")).when(unit).getTime();

        // mock cluster DAO
        VdsGroupDAO vdsGroupDAO = mock(VdsGroupDAO.class);
        doReturn(vdsGroupDAO).when(unit).getVdsGroupDao();
        doReturn(cluster).when(vdsGroupDAO).get(any(Guid.class));

        // mock host DAO
        VdsDAO vdsDAO = mock(VdsDAO.class);
        doReturn(vdsDAO).when(unit).getVdsDao();
        doReturn(new ArrayList(hosts.values())).when(vdsDAO).getAllForVdsGroup(any(Guid.class));

        // mock VM DAO
        VmDAO vmDAO = mock(VmDAO.class);
        doReturn(vmDAO).when(unit).getVmDao();
        for (Guid guid: hosts.keySet()) {
            doReturn(vmsOnAHost(vms.values(), guid)).when(vmDAO).getAllRunningForVds(guid);
        }

        return unit;
    }

    private List<VM> vmsOnAHost(Collection<VM> vms, Guid host) {
        List<VM> result = new ArrayList<>();
        for (VM vm: vms) {
            if (vm.getRunOnVds().equals(host)) {
                result.add(vm);
            }
        }
        return result;
    }
}
