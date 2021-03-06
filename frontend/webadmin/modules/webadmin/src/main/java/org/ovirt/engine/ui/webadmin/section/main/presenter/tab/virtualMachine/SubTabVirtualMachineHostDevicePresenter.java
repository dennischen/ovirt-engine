package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.ovirt.engine.core.common.businessentities.HostDeviceView;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.place.PlaceRequestFactory;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.vms.hostdev.VmHostDeviceListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.VirtualMachineSelectionChangeEvent;

public class SubTabVirtualMachineHostDevicePresenter extends AbstractSubTabPresenter<VM, VmListModel<Void>, VmHostDeviceListModel,
        SubTabVirtualMachineHostDevicePresenter.ViewDef, SubTabVirtualMachineHostDevicePresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.virtualMachineHostDeviceSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabVirtualMachineHostDevicePresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VM> {
    }

    @TabInfo(container = VirtualMachineSubTabPanelPresenter.class)
    static TabData getTabData(ApplicationConstants applicationConstants, SearchableDetailModelProvider<HostDeviceView, VmListModel<Void>, VmHostDeviceListModel> modelProvider) {
        return new ModelBoundTabData(applicationConstants.virtualMachineHostDeviceSubTabLabel(), 4, modelProvider);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return PlaceRequestFactory.get(WebAdminApplicationPlaces.virtualMachineMainTabPlace);
    }

    @Inject
    public SubTabVirtualMachineHostDevicePresenter(EventBus eventBus, ViewDef view, ProxyDef proxy, PlaceManager placeManager,
            SearchableDetailModelProvider<HostDeviceView, VmListModel<Void>, VmHostDeviceListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, VirtualMachineSubTabPanelPresenter.TYPE_SetTabContent);
    }

    @ProxyEvent
    public void onVirtualMachineSelectionChange(VirtualMachineSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }
}
