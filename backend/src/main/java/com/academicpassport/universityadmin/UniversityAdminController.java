package com.academicpassport.universityadmin;

import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.universityadmin.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/university-admin")
@PreAuthorize("hasRole('UNIVERSITY_ADMIN')")
public class UniversityAdminController {

    private final UniversityAdminService service;

    public UniversityAdminController(UniversityAdminService service) {
        this.service = service;
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getDashboardStats(principal));
    }

    // --- Departments ---

    @GetMapping("/departments")
    public ResponseEntity<PageResponse<DepartmentDto>> getDepartments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getDepartments(principal, search, pageable));
    }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentDto> createDepartment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.ok(service.createDepartment(principal, request));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(service.updateDepartment(principal, id, request));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        service.deleteDepartment(principal, id);
        return ResponseEntity.noContent().build();
    }

    // --- Academic Structure (Semester & Subject) ---

    @GetMapping("/departments/{departmentId}/semesters")
    public ResponseEntity<List<SemesterDto>> getSemesters(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(service.getSemesters(principal, departmentId));
    }

    @PostMapping("/departments/{departmentId}/semesters")
    public ResponseEntity<SemesterDto> createSemester(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long departmentId,
            @Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.ok(service.createSemester(principal, departmentId, request));
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<List<SubjectDto>> getSubjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long semesterId) {
        return ResponseEntity.ok(service.getSubjects(principal, semesterId));
    }

    @PostMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<SubjectDto> createSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long semesterId,
            @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(service.createSubject(principal, semesterId, request));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<SubjectDto> updateSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        return ResponseEntity.ok(service.updateSubject(principal, id, request));
    }

    // --- Staff ---

    @GetMapping("/staff")
    public ResponseEntity<PageResponse<StaffDto>> getStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getStaff(principal, search, departmentId, pageable));
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffDto> createStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(service.createStaff(principal, request));
    }

    @PatchMapping("/staff/{id}/status")
    public ResponseEntity<StaffDto> updateStaffStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(service.updateStaffStatus(principal, id, request.getIsActive()));
    }

    // --- Students ---

    @GetMapping("/students")
    public ResponseEntity<PageResponse<StudentDto>> getStudents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getStudents(principal, search, departmentId, pageable));
    }

    @PostMapping("/students")
    public ResponseEntity<StudentDto> createStudent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.ok(service.createStudent(principal, request));
    }

    @PatchMapping("/students/{id}/status")
    public ResponseEntity<StudentDto> updateStudentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(service.updateStudentStatus(principal, id, request.getIsActive()));
    }
}
