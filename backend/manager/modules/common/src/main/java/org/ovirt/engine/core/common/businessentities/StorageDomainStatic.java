package org.ovirt.engine.core.common.businessentities;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.ToStringBuilder;
import org.ovirt.engine.core.common.validation.annotation.ValidDescription;
import org.ovirt.engine.core.common.validation.annotation.ValidName;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;

public class StorageDomainStatic implements BusinessEntity<Guid>, Nameable {
    private static final long serialVersionUID = 8635263021145935458L;

    private Guid id;

    @Size(min = 1, max = BusinessEntitiesDefinitions.STORAGE_SIZE)
    private String storage;

    // TODO storage name needs to be made unique
    @ValidName(message = "VALIDATION.STORAGE_DOMAIN.NAME.INVALID", groups = { CreateEntity.class, UpdateEntity.class })
    @Size(min = 1, max = BusinessEntitiesDefinitions.STORAGE_NAME_SIZE)
    private String name;

    @ValidDescription(message = "VALIDATION.STORAGE_DOMAIN.DESCRIPTION.INVALID", groups = { CreateEntity.class,
            UpdateEntity.class })
    @Size(min = 1, max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE,
            message = "VALIDATION.STORAGE_DOMAIN.DESCRIPTION.MAX",
            groups = { CreateEntity.class, UpdateEntity.class })
    private String description;

    private String comment;

    private StorageDomainType storageType;

    private StorageType storagePoolType;

    private StorageServerConnections connection;

    private StorageFormatType storageFormat;

    private boolean autoRecoverable;

    private boolean containsUnregisteredEntities;

    private SANState sanState;

    private transient long lastTimeUsedAsMaster;

    private Boolean wipeAfterDelete;

    @Min(value = 0, message = "VALIDATION.STORAGE_DOMAIN.WARNING_LOW_SPACE_INDICATOR.RANGE")
    @Max(value = 100, message = "VALIDATION.STORAGE_DOMAIN.WARNING_LOW_SPACE_INDICATOR.RANGE")
    private Integer warningLowSpaceIndicator;

    @Min(value = 0, message = "VALIDATION.STORAGE_DOMAIN.CRITICAL_SPACE_ACTION_BLOCKER.RANGE")
    @Max(value = 100, message = "VALIDATION.STORAGE_DOMAIN.CRITICAL_SPACE_ACTION_BLOCKER.RANGE")
    private Integer criticalSpaceActionBlocker;

    public StorageDomainStatic() {
        id = Guid.Empty;
        storageType = StorageDomainType.Master;
        storagePoolType = StorageType.UNKNOWN;
        autoRecoverable = true;
        name = "";
    }

    @Override
    public Guid getId() {
        return this.id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public StorageDomainType getStorageDomainType() {
        return storageType;
    }

    public void setStorageDomainType(StorageDomainType storageType) {
        this.storageType = storageType;
    }

    public StorageType getStorageType() {
        return storagePoolType;
    }

    public void setStorageType(StorageType storagePoolType) {
        this.storagePoolType = storagePoolType;
    }

    public String getStorageName() {
        return name;
    }

    public void setStorageName(String name) {
        this.name = name;
    }

    public StorageServerConnections getConnection() {
        return connection;
    }

    public void setConnection(StorageServerConnections connection) {
        this.connection = connection;
    }

    public StorageFormatType getStorageFormat() {
        return storageFormat;
    }

    public void setStorageFormat(StorageFormatType storageFormat) {
        this.storageFormat = storageFormat;
    }

    public boolean isAutoRecoverable() {
        return autoRecoverable;
    }

    public void setAutoRecoverable(boolean autoRecoverable) {
        this.autoRecoverable = autoRecoverable;
    }

    public boolean isContainsUnregisteredEntities() {
        return containsUnregisteredEntities;
    }

    public void setContainsUnregisteredEntities(boolean containsUnregisteredEntities) {
        this.containsUnregisteredEntities = containsUnregisteredEntities;
    }

    public long getLastTimeUsedAsMaster() {
        return lastTimeUsedAsMaster;
    }

    public void setLastTimeUsedAsMaster(long lastTimeUsedAsMaster) {
        this.lastTimeUsedAsMaster = lastTimeUsedAsMaster;
    }

    public Boolean getWipeAfterDelete() {
        return wipeAfterDelete;
    }

    public void setWipeAfterDelete(Boolean wipeAfterDelete) {
        this.wipeAfterDelete = wipeAfterDelete;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public SANState getSanState() {
        return sanState;
    }

    public void setSanState(SANState sanState) {
        this.sanState = sanState;
    }

    public Integer getWarningLowSpaceIndicator() {
        return warningLowSpaceIndicator;
    }

    public void setWarningLowSpaceIndicator(Integer warningLowSpaceIndicator) {
        this.warningLowSpaceIndicator = warningLowSpaceIndicator;
    }

    public Integer getCriticalSpaceActionBlocker() {
        return criticalSpaceActionBlocker;
    }

    public void setCriticalSpaceActionBlocker(Integer criticalSpaceActionBlocker) {
        this.criticalSpaceActionBlocker = criticalSpaceActionBlocker;
    }

    @Override
    public String getName() {
        return getStorageName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (autoRecoverable ? 0 : 1);
        result = prime * result + (containsUnregisteredEntities ? 0 : 1);
        result = prime * result + ((connection == null) ? 0 : connection.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((storage == null) ? 0 : storage.hashCode());
        result = prime * result + ((storageFormat == null) ? 0 : storageFormat.hashCode());
        result = prime * result + ((storagePoolType == null) ? 0 : storagePoolType.hashCode());
        result = prime * result + ((storageType == null) ? 0 : storageType.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((sanState == null) ? 0 : sanState.hashCode());
        result = prime * result + ((wipeAfterDelete == null) ? 0 : wipeAfterDelete.hashCode());
        result = prime * result + ((warningLowSpaceIndicator == null) ? 0 : warningLowSpaceIndicator.hashCode());
        result = prime * result + ((criticalSpaceActionBlocker == null) ? 0 : criticalSpaceActionBlocker.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StorageDomainStatic other = (StorageDomainStatic) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && autoRecoverable == other.autoRecoverable
                && containsUnregisteredEntities == other.containsUnregisteredEntities
                && ObjectUtils.objectsEqual(connection, other.connection)
                && ObjectUtils.objectsEqual(name, other.name)
                && ObjectUtils.objectsEqual(storage, other.storage)
                && storageFormat == other.storageFormat
                && storagePoolType == other.storagePoolType
                && storageType == other.storageType
                && sanState == other.sanState
                && ObjectUtils.objectsEqual(wipeAfterDelete, other.wipeAfterDelete)
                && ObjectUtils.objectsEqual(description, other.description))
                && ObjectUtils.objectsEqual(warningLowSpaceIndicator, other.warningLowSpaceIndicator)
                && ObjectUtils.objectsEqual(criticalSpaceActionBlocker, other.criticalSpaceActionBlocker);
    }

    @Override
    public String toString() {
        return ToStringBuilder.forInstance(this)
                .append("name", getName())
                .append("id", getId())
                .build();
    }
}
