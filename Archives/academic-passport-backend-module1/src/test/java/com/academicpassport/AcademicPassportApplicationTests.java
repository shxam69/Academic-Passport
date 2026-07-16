package com.academicpassport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Module 1 scaffold test: verifies the Spring application context starts
 * cleanly with no beans wired yet beyond the framework defaults.
 * <p>
 * This is intentionally the only test in this module. There is no business
 * logic yet to unit test — the thing actually worth proving at this stage
 * is "does the app boot," and that's what this asserts. Every subsequent
 * module adds its own tests alongside its own code; this file does not grow.
 */
@SpringBootTest
class AcademicPassportApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failed context load fails this test on its own.
    }
}
