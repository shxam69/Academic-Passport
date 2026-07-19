package com.academicpassport.college;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {

    Optional<College> findByCollegeCode(String collegeCode);

    boolean existsByCollegeCode(String collegeCode);
    
    @Query("SELECT c FROM College c WHERE " +
           "(:isActive IS NULL OR c.isActive = :isActive) AND " +
           "(:search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.collegeCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<College> searchColleges(
            @Param("isActive") Boolean isActive, 
            @Param("search") String search, 
            org.springframework.data.domain.Pageable pageable);

    /**
     * The ONLY sanctioned way to remove a College. Never call delete()/deleteById()
     * on this repository — those issue a real SQL DELETE and bypass deleted_at
     * entirely (see SoftDeletableEntity javadoc).
     */
    @Modifying
    @Query("UPDATE College c SET c.deletedAt = CURRENT_TIMESTAMP, c.deletedBy = :deletedBy WHERE c.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedBy") Long deletedBy);
}
