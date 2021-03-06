package org.ovirt.engine.core.bll.validator;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;

public class UpgradeHostValidator {

    private VDS host;

    public UpgradeHostValidator(VDS host) {
        this.host = host;
    }

    public ValidationResult hostExists() {
        return ValidationResult.failWith(VdcBllMessages.VDS_INVALID_SERVER_ID).when(host == null);
    }

    public ValidationResult statusSupportedForHostUpgrade() {
        return ValidationResult.failWith(VdcBllMessages.VDS_CANNOT_INSTALL_STATUS_ILLEGAL)
                .unless(VdcActionUtils.canExecute(Arrays.asList(host), VDS.class, VdcActionType.UpgradeHost));
    }

    public ValidationResult statusSupportedForHostUpgradeInternal() {
        return ValidationResult.failWith(VdcBllMessages.VDS_CANNOT_INSTALL_STATUS_ILLEGAL)
                .when(host.getStatus() != VDSStatus.Maintenance);
    }

    public ValidationResult updatesAvailable() {
        return ValidationResult.failWith(VdcBllMessages.NO_AVAILABLE_UPDATES_FOR_HOST)
                .unless(host.getVdsType() == VDSType.oVirtNode || host.isUpdateAvailable());
    }

    public ValidationResult imageProvidedForOvirtNode(String image) {
        return ValidationResult.failWith(VdcBllMessages.VDS_CANNOT_INSTALL_MISSING_IMAGE_FILE)
                .when(host.getVdsType() == VDSType.oVirtNode && StringUtils.isBlank(image));
    }

    public ValidationResult hostWasInstalled() {
        return ValidationResult.failWith(VdcBllMessages.CANNOT_UPGRADE_HOST_WITHOUT_OS)
                .when(host.getHostOs() == null);
    }
}
