package com.academicpassport.college;

import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the soft-delete pattern's least obvious behavior: a soft-deleted row's
 * previously-unique value must become available for reuse (partial unique index),
 * and @SQLRestriction must transparently hide soft-deleted rows from normal finds
 * without any repository method needing to say "AND deletedAt IS NULL" itself.
 */
@Transactional
class CollegeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CollegeRepository collegeRepository;

    @Test
    void findByCollegeCode_excludesSoftDeletedRows() {
        College college = new College();
        college.setName("Test College");
        college.setCollegeCode("TC-" + UUID.randomUUID().toString().substring(0, 8));
        college = collegeRepository.saveAndFlush(college);

        collegeRepository.softDelete(college.getId(), null);
        collegeRepository.flush();

        // @SQLRestriction("deleted_at IS NULL") must make this invisible to a
        // normal finder, even though the row still physically exists in the table.
        Optional<College> found = collegeRepository.findByCollegeCode(college.getCollegeCode());
        assertThat(found).isEmpty();
    }

    @Test
    void collegeCode_canBeReused_afterSoftDelete() {
        String code = "RU-" + UUID.randomUUID().toString().substring(0, 8);

        College first = new College();
        first.setName("Original College");
        first.setCollegeCode(code);
        first = collegeRepository.saveAndFlush(first);
        collegeRepository.softDelete(first.getId(), null);
        collegeRepository.flush();

        // The partial unique index (WHERE deleted_at IS NULL) is the entire point
        // here — a plain UNIQUE constraint would reject this insert even though
        // the original row is soft-deleted. If this throws, the partial index
        // migration regressed to a plain unique constraint.
        College second = new College();
        second.setName("Reissued College Record");
        second.setCollegeCode(code);
        College saved = collegeRepository.saveAndFlush(second);

        assertThat(saved.getId()).isNotEqualTo(first.getId());
        assertThat(collegeRepository.findByCollegeCode(code)).isPresent();
    }

    @Test
    void collegeCode_rejectsDuplicate_amongActiveRows() {
        String code = "DP-" + UUID.randomUUID().toString().substring(0, 8);

        College first = new College();
        first.setName("First");
        first.setCollegeCode(code);
        collegeRepository.saveAndFlush(first);

        College second = new College();
        second.setName("Second");
        second.setCollegeCode(code);

        assertThatThrownBy(() -> collegeRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
