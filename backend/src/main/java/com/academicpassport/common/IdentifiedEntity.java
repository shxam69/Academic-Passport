package com.academicpassport.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Bare identity — just the primary key. Base for anything that doesn't fit the
 * "has both created_at and updated_at" shape (see {@link BaseEntity} for that),
 * e.g. entities whose table only tracks created_at, or tracks neither and uses
 * its own domain-specific timestamp instead (ocr_results.processed_at).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class IdentifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
