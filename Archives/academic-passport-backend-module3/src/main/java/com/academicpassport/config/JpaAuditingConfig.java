package com.academicpassport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code @CreatedDate}/{@code @LastModifiedDate} support for every entity
 * extending {@link com.academicpassport.common.BaseEntity}. Deliberately not wiring
 * an AuditorAware<Long> here yet — nothing in this module needs "who created this
 * row" (that's what deleted_by / staff_id / student_id already capture explicitly
 * per-entity). Add AuditorAware only if a genuine created-by/updated-by need shows
 * up later; don't build it speculatively now.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
