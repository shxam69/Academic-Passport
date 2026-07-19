package com.academicpassport.college.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OnboardingRequest {
    @NotBlank(message = "Institution name is required")
    private String institutionName;
    
    // Address fields
    @NotBlank(message = "Address line is required")
    private String addressLine;
    @NotBlank(message = "City is required")
    private String city;
    @NotBlank(message = "State is required")
    private String state;
    @NotBlank(message = "Postal code is required")
    private String postalCode;
    @NotBlank(message = "Country is required")
    private String country;
    
    // Contact fields
    @NotBlank(message = "Contact name is required")
    private String contactName;
    @NotBlank(message = "Contact email is required")
    private String contactEmail;
    @NotBlank(message = "Contact phone is required")
    private String contactPhone;
    
    // Optional
    private String institutionType;
    private String website;
    
    // The password set by the admin during setup
    @NotBlank(message = "Password is required")
    private String password;
}
