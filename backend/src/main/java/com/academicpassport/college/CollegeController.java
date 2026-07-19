package com.academicpassport.college;

import com.academicpassport.college.dto.CollegeDetailsDto;
import com.academicpassport.college.dto.CollegeDto;
import com.academicpassport.college.dto.CreateCollegeRequest;
import com.academicpassport.college.dto.CreateUniversityAdminRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/colleges")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public Page<CollegeDto> searchColleges(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return collegeService.searchColleges(isActive, search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollegeDto createCollege(@Valid @RequestBody CreateCollegeRequest request) {
        return collegeService.createCollege(request);
    }

    @GetMapping("/{id}")
    public CollegeDetailsDto getCollegeDetails(@PathVariable Long id) {
        return collegeService.getCollegeDetails(id);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCollegeStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        if (!body.containsKey("isActive")) {
            throw new IllegalArgumentException("isActive is required");
        }
        collegeService.updateCollegeStatus(id, body.get("isActive"));
    }

    @PostMapping("/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUniversityAdmin(@PathVariable Long id, @Valid @RequestBody CreateUniversityAdminRequest request) {
        collegeService.createUniversityAdmin(id, request);
    }
}
