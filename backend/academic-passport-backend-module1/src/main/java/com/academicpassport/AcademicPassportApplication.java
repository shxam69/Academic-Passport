package com.academicpassport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Academic Passport — MVP backend entry point.
 * <p>
 * Modular monolith (see /docs/07-folder-structure-and-standards.md for the
 * package-by-feature layout). No microservices — every module lives in this
 * one deployable jar and talks to the others only through service interfaces.
 */
@SpringBootApplication
public class AcademicPassportApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicPassportApplication.class, args);
    }
}
