package com.academicpassport.common;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for entities needing optimistic locking but with NO created_at/updated_at
 * columns at all — marksheet_subjects and verifications each track their own
 * domain-specific timestamp instead (verifications.verified_at), and neither
 * needed a generic audit timestamp on top of that. Extends IdentifiedEntity
 * directly (id + version only), not BaseEntity — extending BaseEntity here
 * would have meant ddl-auto=validate failing at startup against these two
 * tables, which don't have the columns it expects.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class VersionedEntity extends IdentifiedEntity {

    @Version
    private Long version;
}
