package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;

import org.ovirt.engine.api.model.Domain;
import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.core.authentication.DirectoryGroup;
import org.ovirt.engine.core.authentication.DirectoryStub;
import org.ovirt.engine.core.common.queries.DirectoryIdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendDomainGroupResourceTest
    extends AbstractBackendSubResourceTest<Group, DirectoryGroup, BackendDomainGroupResource> {

    public BackendDomainGroupResourceTest() {
        super(new BackendDomainGroupResource(EXTERNAL_IDS[1], null));
    }

    @Override
    protected void init () {
        super.init();
        setUpParentExpectations();
    }

    @Test
    public void testGet() throws Exception {
        UriInfo uriInfo = setUpBasicUriExpectations();
        setUriInfo(uriInfo);
        setUpEntityQueryExpectations(1, false);
        control.replay();
        verifyModel(resource.get(), 1);
    }

    @Override
    protected void verifyModel(Group model, int index) {
        assertEquals(NAMES[index], model.getName());
    }

    @Test
    public void testGetNotFound() throws Exception {
        UriInfo uriInfo = setUpBasicUriExpectations();
        setUriInfo(uriInfo);
        setUpEntityQueryExpectations(1, true);
        control.replay();
        try {
            resource.get();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    private void setUpParentExpectations() {
        BackendDomainGroupsResource parent = control.createMock(BackendDomainGroupsResource.class);
        Domain domain = new Domain();
        domain.setName(DOMAIN);
        expect(parent.getDirectory()).andReturn(domain).anyTimes();
        resource.setParent(parent);
    }

    private void setUpEntityQueryExpectations(int index, boolean notFound) throws Exception {
        setUpGetEntityExpectations(
            VdcQueryType.GetDirectoryGroupById,
            DirectoryIdQueryParameters.class,
            new String[] { "Domain", "Id" },
            new Object[] { DOMAIN, EXTERNAL_IDS[index] },
            notFound? null: getEntity(index)
        );
    }

    @Override
    protected DirectoryGroup getEntity(int index) {
        return new DirectoryGroup(new DirectoryStub(DOMAIN), EXTERNAL_IDS[index], NAMES[index]);
    }
}
