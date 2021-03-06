package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.UserProfileParameters;
import org.ovirt.engine.core.common.businessentities.UserProfile;
import org.ovirt.engine.core.common.errors.VdcBllMessages;


public class RemoveUserProfileCommand<T extends UserProfileParameters> extends UserProfilesOperationCommandBase<T> {

    public RemoveUserProfileCommand(T parameters){
        this(parameters, null);
    }

    public RemoveUserProfileCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected boolean canDoAction() {
        UserProfile dbProfile = userProfileDao.getByUserId(getUserId());
        UserProfile givenProfile = getParameters().getUserProfile();

        if (dbProfile == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_PROFILE_NOT_EXIST);
        }
        if (!dbProfile.getId().equals(givenProfile.getId())) {
            log.warn("Profile ID mismatch: expected in profile {} received {}",
                    dbProfile.getId().toString(), givenProfile.getId().toString());
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_PROFILE_NOT_EXIST);
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_REMOVE_PROFILE : AuditLogType.USER_REMOVE_PROFILE_FAILED;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__USER_PROFILE);
    }

    @Override
    protected void executeCommand() {
        userProfileDao.remove(getParameters().getUserProfile().getId());
        setSucceeded(true);
    }
}
