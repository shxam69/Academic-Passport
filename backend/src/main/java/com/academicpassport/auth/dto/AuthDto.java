package com.academicpassport.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        private String collegeCode;
    }

    @Getter @Setter
    public static class AuthResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private UserDto user;
        
        public AuthResponse(String accessToken, UserDto user) {
            this.accessToken = accessToken;
            this.user = user;
        }
    }

    @Getter @Setter
    public static class UserDto {
        private Long id;
        private String email;
        private String role;
        private Long collegeId;
        
        // Constructor manually mapping properties
        public UserDto(Long id, String email, String role, Long collegeId) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.collegeId = collegeId;
        }
    }

    @Getter @Setter
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
        private String collegeCode;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;
    }

    @Getter @Setter
    public static class VerifyEmailRequest {
        @NotBlank
        private String token;
    }
    
    @Getter @Setter
    public static class ResendVerificationRequest {
        @NotBlank @Email
        private String email;
        private String collegeCode;
    }
}
