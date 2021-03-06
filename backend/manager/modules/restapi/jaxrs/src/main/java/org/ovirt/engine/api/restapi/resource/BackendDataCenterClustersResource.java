package org.ovirt.engine.api.restapi.resource;

import static org.ovirt.engine.api.restapi.resource.BackendDataCenterResource.getStoragePool;

import java.util.List;

import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.Clusters;
import org.ovirt.engine.api.model.DataCenter;
import org.ovirt.engine.api.resource.ClusterResource;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendDataCenterClustersResource extends BackendClustersResource {

    protected Guid dataCenterId;
    public BackendDataCenterClustersResource(String dataCenterId) {
        super();
        this.dataCenterId = asGuid(dataCenterId);
    }

    @Override
    public Clusters list() {
        return mapCollection(getVdsGroups());
    }

    protected List<VDSGroup> getVdsGroups() {
        return getBackendCollection(VdcQueryType.GetVdsGroupsByStoragePoolId,
                    new IdQueryParameters(dataCenterId));
    }

    @Override
    @SingleEntityResource
    public ClusterResource getClusterSubResource(String id) {
        return inject(new BackendDataCenterClusterResource(this, id));
    }

    @Override
    protected String[] getMandatoryParameters() {
        return new String[] { "name" };
    }

    @Override
    protected StoragePool getDataCenter(Cluster cluster) {
        DataCenter dataCenter = new DataCenter();
        dataCenter.setId(dataCenterId.toString());
        cluster.setDataCenter(dataCenter);
        StoragePool pool = getStoragePool(cluster.getDataCenter(), this);
        return pool;
    }

}
