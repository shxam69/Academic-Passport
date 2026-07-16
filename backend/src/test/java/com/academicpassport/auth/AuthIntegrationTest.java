package com.academicpassport.auth;

import com.academicpassport.auth.dto.AuthDto;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CollegeRepository collegeRepository;

    private User testUser;
    private College testCollege;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();

        testCollege = new College();
        testCollege.setName("Test College");
        testCollege.setCollegeCode("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        testCollege = collegeRepository.saveAndFlush(testCollege);

        testUser = new User();
        testUser.setEmail("student@test.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole(UserRole.STUDENT);
        testUser.setCollege(testCollege);
        testUser.setIsActive(true);
        testUser.setIsVerified(true);
        userRepository.save(testUser);
    }

    @Test
    void testLogin_Success() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("student@test.com");
        request.setPassword("password123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        ResponseEntity<AuthDto.AuthResponse> result = authController.login(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getAccessToken()).isNotEmpty();
        
        Cookie cookie = response.getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void testLogin_InvalidCredentials() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("student@test.com");
        request.setPassword("wrongpassword");

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> authController.login(request, response))
                .isInstanceOf(BadCredentialsException.class);
    }
    
    @Test
    void testLogin_UnverifiedAccount() {
        testUser.setIsVerified(false);
        userRepository.save(testUser);
        
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("student@test.com");
        request.setPassword("password123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> authController.login(request, response))
                .isInstanceOf(LockedException.class);
    }

    @Test
    void testRefresh_Success() {
        // 1. Get token
        String rawToken = TokenGeneratorUtil.generateSecureToken();
        RefreshToken rt = new RefreshToken();
        rt.setUser(testUser);
        rt.setTokenHash(TokenHashUtil.hashToken(rawToken));
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setFamilyId(UUID.randomUUID().toString());
        refreshTokenRepository.save(rt);
        
        // 2. Call refresh
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", rawToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        ResponseEntity<AuthDto.AuthResponse> result = authController.refresh(request, response);
        
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getCookie("refreshToken")).isNotNull();
    }

    @Test
    void testRefresh_ReplayFamilyRevocation() {
        // Manually create a revoked token
        String rawToken = TokenGeneratorUtil.generateSecureToken();
        RefreshToken rt = new RefreshToken();
        rt.setUser(testUser);
        rt.setTokenHash(TokenHashUtil.hashToken(rawToken));
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setFamilyId(UUID.randomUUID().toString());
        rt.setRevoked(true); // REVOKED!
        refreshTokenRepository.save(rt);
        
        // Create an active token in the SAME family
        String activeRaw = TokenGeneratorUtil.generateSecureToken();
        RefreshToken activeRt = new RefreshToken();
        activeRt.setUser(testUser);
        activeRt.setTokenHash(TokenHashUtil.hashToken(activeRaw));
        activeRt.setExpiresAt(Instant.now().plusSeconds(3600));
        activeRt.setFamilyId(rt.getFamilyId());
        refreshTokenRepository.save(activeRt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", rawToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        assertThatThrownBy(() -> authController.refresh(request, response))
                .isInstanceOf(BadCredentialsException.class);
                
        // Ensure family was revoked
        RefreshToken refreshedActive = refreshTokenRepository.findById(activeRt.getId()).get();
        assertThat(refreshedActive.getRevoked()).isTrue();
    }
    
    @Test
    void testLogout_Idempotent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "fake123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        ResponseEntity<Void> result = authController.logout(request, response);
                
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getCookie("refreshToken").getMaxAge()).isEqualTo(0);
    }

    @Test
    void testForgotPassword_GenericResponse() {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest();
        request.setEmail("doesnotexist@test.com");
        
        ResponseEntity<Void> response = authController.forgotPassword(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void testPasswordReset_RevokesSessions() {
        String rawToken = TokenGeneratorUtil.generateSecureToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(testUser);
        token.setTokenHash(TokenHashUtil.hashToken(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        passwordResetTokenRepository.save(token);
        
        // Create an active refresh token
        RefreshToken rt = new RefreshToken();
        rt.setUser(testUser);
        rt.setTokenHash(TokenHashUtil.hashToken(TokenGeneratorUtil.generateSecureToken()));
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setFamilyId(UUID.randomUUID().toString());
        refreshTokenRepository.save(rt);
        
        AuthDto.ResetPasswordRequest request = new AuthDto.ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("newpassword123");
        
        ResponseEntity<Void> response = authController.resetPassword(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Check password changed
        User updatedUser = userRepository.findById(testUser.getId()).get();
        assertThat(passwordEncoder.matches("newpassword123", updatedUser.getPasswordHash())).isTrue();
        
        // Check refresh token revoked
        RefreshToken updatedRt = refreshTokenRepository.findById(rt.getId()).get();
        assertThat(updatedRt.getRevoked()).isTrue();
    }
    
    @Test
    void testEmailVerification_ConcurrentConsumption() {
        String rawToken = TokenGeneratorUtil.generateSecureToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(testUser);
        token.setTokenHash(TokenHashUtil.hashToken(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        emailVerificationTokenRepository.save(token);
        
        AuthDto.VerifyEmailRequest request = new AuthDto.VerifyEmailRequest();
        request.setToken(rawToken);
        
        // First consumption
        ResponseEntity<Void> response1 = authController.verifyEmail(request);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Second consumption MUST fail because consumption is atomic
        assertThatThrownBy(() -> authController.verifyEmail(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
