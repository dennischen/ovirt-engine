package org.ovirt.engine.ui.webadmin.section.main.view.popup.cluster;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.AdditionalFeature;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractTabbedModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.EntityModelWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.VisibilityRenderer;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelCheckBoxGroup;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabel;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextAreaLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.form.key_value.KeyValueWidget;
import org.ovirt.engine.ui.common.widget.renderer.BooleanRendererWithNullText;
import org.ovirt.engine.ui.common.widget.renderer.NameRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.SerialNumberPolicyWidget;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.TabName;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.cluster.ClusterPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ClusterPopupView extends AbstractTabbedModelBoundPopupView<ClusterModel> implements ClusterPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ClusterModel, ClusterPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ClusterPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ClusterPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    DialogTabPanel tabPanel;

    @UiField
    WidgetStyle style;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    FlowPanel dataCenterPanel;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId
    ListModelListBoxEditor<StoragePool> dataCenterEditor;

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    StringEntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId
    StringEntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Path(value = "comment.entity")
    @WithElementId
    StringEntityModelTextBoxEditor commentEditor;

    @UiField(provided = true)
    @Path(value = "managementNetwork.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Network> managementNetworkEditor;

    @UiField(provided = true)
    @Path(value = "CPU.selectedItem")
    @WithElementId
    ListModelListBoxEditor<ServerCpu> cpuEditor;

    @UiField(provided = true)
    @Path(value = "version.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Version> versionEditor;

    @UiField(provided = true)
    @Path(value = "architecture.selectedItem")
    @WithElementId
    ListModelListBoxEditor<ArchitectureType> architectureEditor;

    @UiField
    @Ignore
    VerticalPanel servicesCheckboxPanel;

    @UiField
    @Path(value = "enableOvirtService.entity")
    @WithElementId("enableOvirtService")
    EntityModelCheckBoxEditor enableOvirtServiceEditor;

    @UiField
    @Path(value = "enableGlusterService.entity")
    @WithElementId("enableGlusterService")
    EntityModelCheckBoxEditor enableGlusterServiceEditor;

    @UiField
    @Ignore
    VerticalPanel servicesRadioPanel;

    @UiField(provided = true)
    @Path(value = "enableOvirtService.entity")
    @WithElementId("enableOvirtServiceOption")
    EntityModelRadioButtonEditor enableOvirtServiceOptionEditor;

    @UiField(provided = true)
    @Path(value = "enableGlusterService.entity")
    @WithElementId("enableGlusterServiceOption")
    EntityModelRadioButtonEditor enableGlusterServiceOptionEditor;

    @UiField(provided = true)
    @Path(value = "isImportGlusterConfiguration.entity")
    @WithElementId("isImportGlusterConfiguration")
    EntityModelCheckBoxEditor importGlusterConfigurationEditor;

    @UiField
    @Ignore
    Label importGlusterExplanationLabel;

    @UiField
    @Path(value = "glusterHostAddress.entity")
    @WithElementId
    StringEntityModelTextBoxEditor glusterHostAddressEditor;

    @UiField
    @Path(value = "glusterHostFingerprint.entity")
    @WithElementId
    StringEntityModelTextAreaLabelEditor glusterHostFingerprintEditor;

    @UiField
    @Path(value = "glusterHostPassword.entity")
    @WithElementId
    StringEntityModelPasswordBoxEditor glusterHostPasswordEditor;

    @UiField
    @Ignore
    Label messageLabel;

    @UiField(provided = true)
    @Path(value = "enableOptionalReason.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableOptionalReasonEditor;

    @UiField(provided = true)
    @Path(value = "enableHostMaintenanceReason.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableHostMaintenanceReasonEditor;

    @UiField
    @Ignore
    Label rngLabel;

    @UiField(provided = true)
    @Path(value = "rngRandomSourceRequired.entity")
    @WithElementId
    EntityModelCheckBoxEditor rngRandomSourceRequired;

    @UiField(provided = true)
    @Path(value = "rngHwrngSourceRequired.entity")
    @WithElementId
    EntityModelCheckBoxEditor rngHwrngSourceRequired;

    @UiField
    @Path(value = "glusterTunedProfile.selectedItem")
    @WithElementId
    ListModelListBoxEditor<String> glusterTunedProfileEditor;

    @UiField(provided = true)
    @Path(value = "additionalClusterFeatures.selectedItem")
    @WithElementId
    ListModelCheckBoxGroup<AdditionalFeature> additionalFeaturesEditor;

    @UiField
    @Ignore
    AdvancedParametersExpander additionalFeaturesExpander;

    @UiField
    @Ignore
    FlowPanel additionalFeaturesExpanderContent;

    @UiField
    @WithElementId
    DialogTab optimizationTab;

    @UiField
    @Ignore
    Label memoryOptimizationPanelTitle;

    @UiField(provided = true)
    InfoIcon memoryOptimizationInfo;

    @UiField(provided = true)
    InfoIcon allowOverbookingInfoIcon;

    @UiField(provided = true)
    @Path(value = "optimizationNone_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationNoneEditor;

    @UiField(provided = true)
    @Path(value = "optimizationForServer_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationForServerEditor;

    @UiField(provided = true)
    @Path(value = "optimizationForDesktop_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationForDesktopEditor;

    @UiField(provided = true)
    @Path(value = "optimizationCustom_IsSelected.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizationCustomEditor;

    @UiField
    FlowPanel cpuThreadsPanel;

    @UiField
    @Ignore
    Label cpuThreadsPanelTitle;

    @UiField(provided = true)
    InfoIcon cpuThreadsInfo;

    @UiField(provided = true)
    @Path(value = "countThreadsAsCores.entity")
    @WithElementId
    EntityModelCheckBoxEditor countThreadsAsCoresEditor;

    @UiField
    @WithElementId
    DialogTab resiliencePolicyTab;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_YES.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_YESEditor;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_HA_ONLY.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_HA_ONLYEditor;

    @UiField(provided = true)
    @Path(value = "migrateOnErrorOption_NO.entity")
    @WithElementId
    EntityModelRadioButtonEditor migrateOnErrorOption_NOEditor;

    @UiField
    @WithElementId
    DialogTab clusterPolicyTab;

    @UiField
    @Ignore
    Label additionPropsPanelTitle;

    @UiField(provided = true)
    @Path(value = "enableTrustedService.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableTrustedServiceEditor;

    @UiField(provided = true)
    @Path(value = "enableHaReservation.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableHaReservationEditor;


    @UiField(provided = true)
    @Path(value = "clusterPolicy.selectedItem")
    @WithElementId
    ListModelListBoxEditor<ClusterPolicy> clusterPolicyEditor;

    @UiField
    @Ignore
    protected KeyValueWidget<KeyValueModel> customPropertiesSheetEditor;

    @UiField(provided = true)
    @Path(value = "enableKsm.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableKsm;

    @UiField(provided = true)
    @Path(value = "enableBallooning.entity")
    @WithElementId
    EntityModelCheckBoxEditor enableBallooning;

    @UiField
    @Ignore
    Label schedulerOptimizationPanelTitle;

    @UiField(provided = true)
    InfoIcon schedulerOptimizationInfoIcon;

    @UiField(provided = true)
    @Path(value = "optimizeForUtilization.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizeForUtilizationEditor;

    @UiField(provided = true)
    @Path(value = "optimizeForSpeed.entity")
    @WithElementId
    EntityModelRadioButtonEditor optimizeForSpeedEditor;

    @UiField
    HorizontalPanel allowOverbookingPanel;

    @UiField(provided = true)
    @Path(value = "guarantyResources.entity")
    @WithElementId
    EntityModelRadioButtonEditor guarantyResourcesEditor;

    @UiField(provided = true)
    @Path(value = "allowOverbooking.entity")
    @WithElementId
    EntityModelRadioButtonEditor allowOverbookingEditor;

    @UiField
    @Ignore
    @WithElementId("serialNumberPolicy")
    SerialNumberPolicyWidget serialNumberPolicyEditor;

    @UiField(provided = true)
    @Path("autoConverge.selectedItem")
    @WithElementId("autoConverge")
    ListModelListBoxEditor<Boolean> autoConvergeEditor;

    @UiField(provided = true)
    @Path("migrateCompressed.selectedItem")
    @WithElementId("migrateCompressed")
    ListModelListBoxEditor<Boolean> migrateCompressedEditor;

    @UiField
    @Ignore
    DialogTab consoleTab;

    @UiField
    @Path(value = "spiceProxy.entity")
    @WithElementId
    StringEntityModelTextBoxEditor spiceProxyEditor;

    @UiField(provided = true)
    @Ignore
    EntityModelWidgetWithInfo spiceProxyEnabledCheckboxWithInfoIcon;

    @Path(value = "spiceProxyEnabled.entity")
    @WithElementId
    EntityModelCheckBoxOnlyEditor spiceProxyOverrideEnabled;

    @UiField
    @Ignore
    DialogTab fencingPolicyTab;

    @UiField(provided = true)
    InfoIcon fencingEnabledInfo;

    @UiField(provided = true)
    @Path(value = "fencingEnabledModel.entity")
    @WithElementId
    EntityModelCheckBoxEditor fencingEnabledCheckBox;

    @UiField(provided = true)
    InfoIcon skipFencingIfSDActiveInfo;

    @UiField(provided = true)
    @Path(value = "skipFencingIfSDActiveEnabled.entity")
    @WithElementId
    EntityModelCheckBoxEditor skipFencingIfSDActiveCheckBox;

    @UiField(provided = true)
    InfoIcon skipFencingIfConnectivityBrokenInfo;

    @UiField(provided = true)
    @Path(value = "skipFencingIfConnectivityBrokenEnabled.entity")
    @WithElementId
    EntityModelCheckBoxEditor skipFencingIfConnectivityBrokenCheckBox;

    @UiField(provided = true)
    @Path(value = "hostsWithBrokenConnectivityThreshold.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Integer> hostsWithBrokenConnectivityThresholdEditor;

    private final Driver driver = GWT.create(Driver.class);

    private final static ApplicationTemplates templates = AssetProvider.getTemplates();
    private final static ApplicationResources resources = AssetProvider.getResources();
    private final static ApplicationConstants constants = AssetProvider.getConstants();
    private final static ApplicationMessages messages = AssetProvider.getMessages();

    @Inject
    public ClusterPopupView(EventBus eventBus) {
        super(eventBus);
        initListBoxEditors();
        initRadioButtonEditors();
        initCheckBoxEditors();
        initInfoIcons();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initAdditionalFeaturesExpander();

        serialNumberPolicyEditor.setRenderer(new VisibilityRenderer.SimpleVisibilityRenderer());

        addStyles();
        localize();
        driver.initialize(this);
        applyModeCustomizations();
        setVisibilities();

        additionalFeaturesEditor.clearAllSelections();
    }

    private void setVisibilities() {
        rngLabel.setVisible(ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly));
    }

    @Override
    protected void populateTabMap() {
        getTabNameMapping().put(TabName.GENERAL_TAB, this.generalTab);
        getTabNameMapping().put(TabName.CONSOLE_TAB, this.consoleTab);
        getTabNameMapping().put(TabName.CLUSTER_POLICY_TAB, this.clusterPolicyTab);
        getTabNameMapping().put(TabName.OPTIMIZATION_TAB, this.optimizationTab);
        getTabNameMapping().put(TabName.RESILIENCE_POLICY_TAB, this.resiliencePolicyTab);
    }

    private void addStyles() {
        importGlusterConfigurationEditor.addContentWidgetContainerStyleName(style.editorContentWidget());
        migrateOnErrorOption_NOEditor.addContentWidgetContainerStyleName(style.label());
        migrateOnErrorOption_YESEditor.addContentWidgetContainerStyleName(style.label());
        migrateOnErrorOption_HA_ONLYEditor.addContentWidgetContainerStyleName(style.label());

        countThreadsAsCoresEditor.setContentWidgetContainerStyleName(style.fullWidth());
        enableTrustedServiceEditor.setContentWidgetContainerStyleName(style.fullWidth());
        enableHaReservationEditor.setContentWidgetContainerStyleName(style.fullWidth());
        enableOptionalReasonEditor.setContentWidgetContainerStyleName(style.fullWidth());
        enableHostMaintenanceReasonEditor.setContentWidgetContainerStyleName(style.fullWidth());
        additionalFeaturesExpanderContent.setStyleName(style.additionalFeaturesExpanderContent());
    }

    private void localize() {
        generalTab.setLabel(constants.clusterPopupGeneralTabLabel());

        dataCenterEditor.setLabel(constants.clusterPopupDataCenterLabel());
        nameEditor.setLabel(constants.clusterPopupNameLabel());
        descriptionEditor.setLabel(constants.clusterPopupDescriptionLabel());
        commentEditor.setLabel(constants.commentLabel());
        managementNetworkEditor.setLabel(constants.managementNetworkLabel());
        cpuEditor.setLabel(constants.clusterPopupCPUTypeLabel());
        architectureEditor.setLabel(constants.clusterPopupArchitectureLabel());
        versionEditor.setLabel(constants.clusterPopupVersionLabel());
        enableOvirtServiceEditor.setLabel(constants.clusterEnableOvirtServiceLabel());
        enableGlusterServiceEditor.setLabel(constants.clusterEnableGlusterServiceLabel());
        enableOvirtServiceOptionEditor.setLabel(constants.clusterEnableOvirtServiceLabel());
        enableGlusterServiceOptionEditor.setLabel(constants.clusterEnableGlusterServiceLabel());
        glusterTunedProfileEditor.setLabel(constants.glusterTunedProfileLabel());
        importGlusterConfigurationEditor.setLabel(constants.clusterImportGlusterConfigurationLabel());
        importGlusterExplanationLabel.setText(constants.clusterImportGlusterConfigurationExplanationLabel());
        glusterHostAddressEditor.setLabel(constants.hostPopupHostAddressLabel());
        glusterHostFingerprintEditor.setLabel(constants.hostPopupHostFingerprintLabel());
        glusterHostPasswordEditor.setLabel(constants.hostPopupPasswordLabel());
        additionalFeaturesExpander.setTitleWhenCollapsed(constants.addtionalClusterFeaturesTitle());
        additionalFeaturesExpander.setTitleWhenExpanded(constants.addtionalClusterFeaturesTitle());

        rngLabel.setText(constants.requiredRngSources());
        rngRandomSourceRequired.setLabel(constants.rngSourceRandom());
        rngHwrngSourceRequired.setLabel(constants.rngSourceHwrng());

        optimizationTab.setLabel(constants.clusterPopupOptimizationTabLabel());

        memoryOptimizationPanelTitle.setText(constants.clusterPopupMemoryOptimizationPanelTitle());
        optimizationNoneEditor.setLabel(constants.clusterPopupOptimizationNoneLabel());

        cpuThreadsPanelTitle.setText(constants.clusterPopupCpuThreadsPanelTitle());
        countThreadsAsCoresEditor.setLabel(constants.clusterPopupCountThreadsAsCoresLabel());

        resiliencePolicyTab.setLabel(constants.clusterPopupResiliencePolicyTabLabel());

        migrateOnErrorOption_YESEditor.setLabel(constants.clusterPopupMigrateOnError_YesLabel());
        migrateOnErrorOption_HA_ONLYEditor.setLabel(constants.clusterPopupMigrateOnError_HaLabel());
        migrateOnErrorOption_NOEditor.setLabel(constants.clusterPopupMigrateOnError_NoLabel());

        clusterPolicyTab.setLabel(constants.clusterPopupClusterPolicyTabLabel());

        additionPropsPanelTitle.setText(constants.clusterPolicyAdditionalPropsPanelTitle());
        enableTrustedServiceEditor.setLabel(constants.clusterPolicyEnableTrustedServiceLabel());
        enableHaReservationEditor.setLabel(constants.clusterPolicyEnableHaReservationLabel());
        enableOptionalReasonEditor.setLabel(constants.clusterPolicyEnableReasonLabel());
        enableHostMaintenanceReasonEditor.setLabel(constants.clusterPolicyEnableHostMaintenanceReasonLabel());
        clusterPolicyEditor.setLabel(constants.clusterPolicySelectPolicyLabel());

        enableKsm.setLabel(constants.enableKsmLabel());
        enableBallooning.setLabel(constants.enableBallooningLabel());

        schedulerOptimizationPanelTitle.setText(constants.schedulerOptimizationPanelLabel());
        optimizeForUtilizationEditor.setLabel(constants.optimizeForUtilizationLabel());
        optimizeForSpeedEditor.setLabel(constants.optimizeForSpeedLabel());
        guarantyResourcesEditor.setLabel(constants.guarantyResourcesLabel());
        allowOverbookingEditor.setLabel(constants.allowOverbookingLabel());

        spiceProxyEditor.setLabel(constants.overriddenSpiceProxyAddress());

        consoleTab.setLabel(constants.consoleTabLabel());

        fencingPolicyTab.setLabel(constants.fencingPolicyTabLabel());

        fencingEnabledCheckBox.setLabel(constants.fencingEnabled());
        skipFencingIfSDActiveCheckBox.setLabel(constants.skipFencingIfSDActive());
        skipFencingIfConnectivityBrokenCheckBox.setLabel(constants.skipFencingWhenConnectivityBroken());
        hostsWithBrokenConnectivityThresholdEditor.setLabel(constants.hostsWithBrokenConnectivityThresholdLabel());
    }

    private void initRadioButtonEditors() {
        enableOvirtServiceOptionEditor = new EntityModelRadioButtonEditor("service"); //$NON-NLS-1$
        enableGlusterServiceOptionEditor = new EntityModelRadioButtonEditor("service"); //$NON-NLS-1$

        optimizationNoneEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationForServerEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationForDesktopEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        optimizationCustomEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$

        migrateOnErrorOption_YESEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        migrateOnErrorOption_HA_ONLYEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        migrateOnErrorOption_NOEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$

        optimizeForUtilizationEditor = new EntityModelRadioButtonEditor("3"); //$NON-NLS-1$
        optimizeForSpeedEditor = new EntityModelRadioButtonEditor("3"); //$NON-NLS-1$

        guarantyResourcesEditor = new EntityModelRadioButtonEditor("4"); //$NON-NLS-1$
        allowOverbookingEditor = new EntityModelRadioButtonEditor("4"); //$NON-NLS-1$
    }

    private void initListBoxEditors() {
        dataCenterEditor = new ListModelListBoxEditor<>(new NameRenderer<StoragePool>());

        managementNetworkEditor = new ListModelListBoxEditor<Network>(new NullSafeRenderer<Network>() {
            @Override
            protected String renderNullSafe(Network network) {
                return network.getName();
            }
        });

        cpuEditor = new ListModelListBoxEditor<ServerCpu>(new NullSafeRenderer<ServerCpu>() {
            @Override
            public String renderNullSafe(ServerCpu object) {
                return object.getCpuName();
            }
        });

        versionEditor = new ListModelListBoxEditor<Version>(new NullSafeRenderer<Version>() {
            @Override
            public String renderNullSafe(Version object) {
                return object.toString();
            }
        });

        architectureEditor = new ListModelListBoxEditor<ArchitectureType>(new NullSafeRenderer<ArchitectureType>() {
            @Override
            public String renderNullSafe(ArchitectureType object) {
                return object.toString();
            }
        });

        clusterPolicyEditor = new ListModelListBoxEditor<>(new NameRenderer<ClusterPolicy>());
        hostsWithBrokenConnectivityThresholdEditor = new ListModelListBoxEditor<Integer>(new NullSafeRenderer<Integer>() {
            @Override
            public String renderNullSafe(Integer object) {
                return object.toString();
            }
        });
        hostsWithBrokenConnectivityThresholdEditor.getContentWidgetContainer().setWidth("75px"); //$NON-NLS-1$

        autoConvergeEditor = new ListModelListBoxEditor<Boolean>(
                new BooleanRendererWithNullText(constants.autoConverge(), constants.dontAutoConverge(), constants.inheritFromGlobal()));

        migrateCompressedEditor = new ListModelListBoxEditor<Boolean>(
                new BooleanRendererWithNullText(constants.compress(), constants.dontCompress(), constants.inheritFromGlobal()));
    }

    private void initCheckBoxEditors() {
        importGlusterConfigurationEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        countThreadsAsCoresEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        enableTrustedServiceEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        enableHaReservationEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        enableOptionalReasonEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        enableHostMaintenanceReasonEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        enableKsm = new EntityModelCheckBoxEditor(Align.RIGHT);
        enableKsm.getContentWidgetContainer().setWidth("350px"); //$NON-NLS-1$

        enableBallooning = new EntityModelCheckBoxEditor(Align.RIGHT);
        enableBallooning.getContentWidgetContainer().setWidth("350px"); //$NON-NLS-1$

        rngRandomSourceRequired = new EntityModelCheckBoxEditor(Align.RIGHT);
        rngHwrngSourceRequired = new EntityModelCheckBoxEditor(Align.RIGHT);

        fencingEnabledCheckBox = new EntityModelCheckBoxEditor(Align.RIGHT);
        fencingEnabledCheckBox.getContentWidgetContainer().setWidth("150px"); //$NON-NLS-1$

        skipFencingIfSDActiveCheckBox = new EntityModelCheckBoxEditor(Align.RIGHT);
        skipFencingIfSDActiveCheckBox.getContentWidgetContainer().setWidth("450px"); //$NON-NLS-1$

        skipFencingIfConnectivityBrokenCheckBox = new EntityModelCheckBoxEditor(Align.RIGHT);
        skipFencingIfConnectivityBrokenCheckBox.getContentWidgetContainer().setWidth("420px"); //$NON-NLS-1$

        additionalFeaturesEditor = new ListModelCheckBoxGroup<>(new AbstractRenderer<AdditionalFeature>() {
            @Override
            public String render(AdditionalFeature feature) {
                return feature.getDescription();
            }
        });
    }

    private void initInfoIcons() {
        memoryOptimizationInfo = new InfoIcon(templates.italicFixedWidth("465px", constants.clusterPopupMemoryOptimizationInfo())); //$NON-NLS-1$

        cpuThreadsInfo = new InfoIcon(templates.italicFixedWidth("600px", constants.clusterPopupCpuThreadsInfo())); //$NON-NLS-1$

        schedulerOptimizationInfoIcon = new InfoIcon(SafeHtmlUtils.EMPTY_SAFE_HTML);
        allowOverbookingInfoIcon = new InfoIcon(SafeHtmlUtils.EMPTY_SAFE_HTML);

        StringEntityModelLabel label = new StringEntityModelLabel();
        label.setText(constants.clusterSpiceProxyEnable());
        label.setWidth("250px"); //$NON-NLS-1$
        spiceProxyOverrideEnabled = new EntityModelCheckBoxOnlyEditor();
        spiceProxyEnabledCheckboxWithInfoIcon = new EntityModelWidgetWithInfo<String>(label, spiceProxyOverrideEnabled);

        fencingEnabledInfo = new InfoIcon(
                templates.italicFixedWidth("400px", constants.fencingEnabledInfo())); //$NON-NLS-1$
        skipFencingIfSDActiveInfo = new InfoIcon(
                templates.italicFixedWidth("400px", constants.skipFencingIfSDActiveInfo())); //$NON-NLS-1$

        skipFencingIfConnectivityBrokenInfo = new InfoIcon(
                templates.italicFixedWidth("400px", constants.skipFencingWhenConnectivityBrokenInfo())); //$NON-NLS-1$
    }

    @Override
    public void setSpiceProxyOverrideExplanation(String explanation) {
        spiceProxyEnabledCheckboxWithInfoIcon.setExplanation(templates.italicText(explanation));
    }

    private void applyModeCustomizations() {
        if (ApplicationModeHelper.getUiMode() == ApplicationMode.GlusterOnly) {
            optimizationTab.setVisible(false);
            resiliencePolicyTab.setVisible(false);
            clusterPolicyTab.setVisible(false);
            consoleTab.setVisible(false);
            fencingPolicyTab.setVisible(false);
            dataCenterPanel.addStyleName(style.generalTabTopDecoratorEmpty());
        }
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    @Override
    public void edit(final ClusterModel object) {
        driver.edit(object);
        customPropertiesSheetEditor.edit(object.getCustomPropertySheet());

        servicesCheckboxPanel.setVisible(object.getAllowClusterWithVirtGlusterEnabled());
        servicesRadioPanel.setVisible(!object.getAllowClusterWithVirtGlusterEnabled());

        serialNumberPolicyEditor.edit(object.getSerialNumberPolicy());

        optimizationForServerFormatter(object);
        optimizationForDesktopFormatter(object);
        optimizationCustomFormatter(object);

        object.getOptimizationForServer().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                optimizationForServerFormatter(object);
            }
        });

        object.getOptimizationForDesktop().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                optimizationForDesktopFormatter(object);
            }
        });

        object.getOptimizationCustom_IsSelected().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                if (object.getOptimizationCustom_IsSelected().getEntity()) {
                    optimizationCustomFormatter(object);
                    optimizationCustomEditor.setVisible(true);
                }
            }
        });

        object.getDataCenter().getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                resiliencePolicyTab.setVisible(object.getisResiliencePolicyTabAvailable());
                applyModeCustomizations();
            }
        });

        object.getEnableGlusterService().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                importGlusterExplanationLabel.setVisible(object.getEnableGlusterService().getEntity()
                        && object.getIsNew());
            }
        });
        importGlusterExplanationLabel.setVisible(object.getEnableGlusterService().getEntity()
                && object.getIsNew());

        object.getVersionSupportsCpuThreads().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                cpuThreadsPanel.setVisible(object.getVersionSupportsCpuThreads().getEntity());
            }
        });

        schedulerOptimizationInfoIcon.setText(SafeHtmlUtils.fromTrustedString(
                templates.italicFixedWidth("350px", //$NON-NLS-1$
                object.getSchedulerOptimizationInfoMessage()).asString()
                        .replaceAll("(\r\n|\n)", "<br />"))); //$NON-NLS-1$ //$NON-NLS-2$
        allowOverbookingInfoIcon.setText(SafeHtmlUtils.fromTrustedString(
                templates.italicFixedWidth("350px", //$NON-NLS-1$
                        object.getAllowOverbookingInfoMessage()).asString()
                        .replaceAll("(\r\n|\n)", "<br />"))); //$NON-NLS-1$ //$NON-NLS-2$
        allowOverbookingPanel.setVisible(allowOverbookingEditor.isVisible());

        object.getVersion().getPropertyChangedEvent().addListener(new IEventListener<PropertyChangedEventArgs>() {
            @Override
            public void eventRaised(Event<? extends PropertyChangedEventArgs> ev, Object sender, PropertyChangedEventArgs args) {
                if (object.getVersion().getSelectedItem() != null) {
                    String clusterVersion = object.getVersion().getSelectedItem().getValue();
                    serialNumberPolicyEditor.setVisible(AsyncDataProvider.getInstance().isSerialNumberPolicySupported(clusterVersion));
                }
            }
        });

        object.getAdditionalClusterFeatures().getItemsChangedEvent().addListener(new IEventListener<EventArgs>() {

            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                List<List<AdditionalFeature>> items = (List<List<AdditionalFeature>>) object.getAdditionalClusterFeatures().getItems();
                // Hide the fields if there is no feature to show
                additionalFeaturesExpander.setVisible(!items.get(0).isEmpty());
                additionalFeaturesExpanderContent.setVisible(!items.get(0).isEmpty());
            }
        });
    }

    private void optimizationForServerFormatter(ClusterModel object) {
        if (object.getOptimizationForServer() != null
                && object.getOptimizationForServer().getEntity() != null) {
            optimizationForServerEditor.setLabel(messages.clusterPopupMemoryOptimizationForServerLabel(
                            object.getOptimizationForServer().getEntity().toString()));
        }
    }

    private void optimizationForDesktopFormatter(ClusterModel object) {
        if (object.getOptimizationForDesktop() != null
                && object.getOptimizationForDesktop().getEntity() != null) {
            optimizationForDesktopEditor.setLabel(messages.clusterPopupMemoryOptimizationForDesktopLabel(
                            object.getOptimizationForDesktop().getEntity().toString()));
        }
    }

    private void optimizationCustomFormatter(ClusterModel object) {
        if (object.getOptimizationCustom() != null
                && object.getOptimizationCustom().getEntity() != null) {
            // Use current value because object.getOptimizationCustom.getEntity() can be null
            optimizationCustomEditor.setLabel(messages.clusterPopupMemoryOptimizationCustomLabel(
                    String.valueOf(object.getMemoryOverCommit())));
        }
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    @Override
    public ClusterModel flush() {
        serialNumberPolicyEditor.flush();
        return driver.flush();
    }

    @Override
    public void allowClusterWithVirtGlusterEnabled(boolean value) {
        servicesCheckboxPanel.setVisible(value);
        servicesRadioPanel.setVisible(!value);
    }

    interface WidgetStyle extends CssResource {
        String label();

        String generalTabTopDecoratorEmpty();

        String editorContentWidget();

        String fullWidth();

        String timeTextBoxEditorWidget();

        String optimizationTabPanel();

        String additionalFeaturesExpanderContent();
    }

    @Override
    public DialogTabPanel getTabPanel() {
        return tabPanel;
    }

    private void initAdditionalFeaturesExpander() {
        additionalFeaturesExpander.initWithContent(additionalFeaturesExpanderContent.getElement());
    }
}
