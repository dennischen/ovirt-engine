package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.businessentities.VmIconDefault;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VmIconIdSizePair;
import org.ovirt.engine.core.dao.VmIconDefaultDao;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * It provides mapping of operating systems to their default icons.
 */
public class GetVmIconDefaultsQuery extends QueriesCommandBase<VdcQueryParametersBase> {

    @Inject
    private VmIconDefaultDao vmIconDefaultDao;

    public GetVmIconDefaultsQuery(VdcQueryParametersBase parameters) {
        super(parameters);
    }

    /**
     * query return type {@code Map<Integer, Guid>} osId -> iconId
     */
    @Override
    protected void executeQueryCommand() {
        final Map<Integer, VmIconIdSizePair> result = new HashMap<>();
        final List<VmIconDefault> iconDefaults = vmIconDefaultDao.getAll();
        for (VmIconDefault iconDefault : iconDefaults) {
            result.put(iconDefault.getOsId(),
                    new VmIconIdSizePair(iconDefault.getSmallIconId(), iconDefault.getLargeIconId()));
        }
        setReturnValue(result);
    }
}
