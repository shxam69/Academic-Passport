package com.academicpassport.common;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for entities needing both soft delete and optimistic locking: users,
 * students, staff, marksheets. These are the rows most likely to see genuine
 * concurrent writes (self-service edits racing admin/staff actions) while also
 * being business records where history must survive a delete.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class VersionedSoftDeletableEntity extends SoftDeletableEntity {

    @Version
    private Long version;
}
