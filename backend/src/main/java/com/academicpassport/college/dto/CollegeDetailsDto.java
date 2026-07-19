package com.academicpassport.college.dto;

import com.academicpassport.auth.User;
import com.academicpassport.college.College;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class CollegeDetailsDto extends CollegeDto {
    private List<AdminDto> admins;
    
    @Getter
    @Setter
    public static class AdminDto {
        private Long id;
        private String email;
        private String mobile;
        private Boolean isActive;
        
        public static AdminDto fromEntity(User user) {
            AdminDto dto = new AdminDto();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setMobile(user.getMobile());
            dto.setIsActive(user.getIsActive());
            return dto;
        }
    }
    
    public static CollegeDetailsDto fromEntityWithAdmins(College college, List<User> admins) {
        CollegeDetailsDto dto = new CollegeDetailsDto();
        dto.setId(college.getId());
        dto.setName(college.getName());
        dto.setCollegeCode(college.getCollegeCode());
        dto.setAddress(college.getAddress());
        dto.setIsActive(college.getIsActive());
        dto.setCreatedAt(college.getCreatedAt());
        if (admins != null) {
            dto.setAdmins(admins.stream().map(AdminDto::fromEntity).collect(Collectors.toList()));
        }
        return dto;
    }
}
