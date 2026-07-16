package com.academicpassport.auth;

import com.academicpassport.auth.dto.AuthDto;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CollegeRepository collegeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.security.jwt.refresh-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs;

    public AuthService(UserRepository userRepository,
                       CollegeRepository collegeRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.collegeRepository = collegeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResult login(AuthDto.LoginRequest request) {
        List<User> users = userRepository.findByEmail(request.getEmail().toLowerCase());
        
        if (users.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user;
        if (users.size() > 1) {
            if (!StringUtils.hasText(request.getCollegeCode())) {
                throw new BadCredentialsException("Multiple accounts found, please specify college code");
            }
            user = users.stream()
                    .filter(u -> u.getCollege() != null && u.getCollege().getCollegeCode().equals(request.getCollegeCode()))
                    .findFirst()
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        } else {
            user = users.get(0);
            if (StringUtils.hasText(request.getCollegeCode())) {
                if (user.getCollege() == null || !user.getCollege().getCollegeCode().equals(request.getCollegeCode())) {
                    throw new BadCredentialsException("Invalid credentials");
                }
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new DisabledException("Account is disabled");
        }

        // Only SUPER_ADMIN bypasses email verification
        if (user.getRole() != UserRole.SUPER_ADMIN && !user.getIsVerified()) {
            throw new LockedException("Email is not verified");
        }

        String accessToken = jwtService.generateToken(user);
        
        String rawRefreshToken = TokenGeneratorUtil.generateSecureToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHashUtil.hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtRefreshExpirationMs));
        // A new login starts a new token family
        refreshToken.setFamilyId(UUID.randomUUID().toString());
        refreshTokenRepository.save(refreshToken);

        AuthDto.UserDto userDto = new AuthDto.UserDto(
                user.getId(), user.getEmail(), user.getRole().name(),
                user.getCollege() != null ? user.getCollege().getId() : null);

        return new LoginResult(new AuthDto.AuthResponse(accessToken, userDto), rawRefreshToken);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResult refresh(String incomingRefreshToken) {
        String tokenHash = TokenHashUtil.hashToken(incomingRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedToken.getRevoked()) {
            // Replay detected! Revoke the entire family
            refreshTokenRepository.revokeFamily(storedToken.getFamilyId());
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        // Revoke the used token (Rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (!user.getIsActive()) {
            throw new DisabledException("Account is disabled");
        }

        String newAccessToken = jwtService.generateToken(user);
        
        String newRawRefreshToken = TokenGeneratorUtil.generateSecureToken();
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(TokenHashUtil.hashToken(newRawRefreshToken));
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(jwtRefreshExpirationMs));
        newRefreshToken.setFamilyId(storedToken.getFamilyId()); // Maintain family ID
        refreshTokenRepository.save(newRefreshToken);

        AuthDto.UserDto userDto = new AuthDto.UserDto(
                user.getId(), user.getEmail(), user.getRole().name(),
                user.getCollege() != null ? user.getCollege().getId() : null);

        return new LoginResult(new AuthDto.AuthResponse(newAccessToken, userDto), newRawRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return; // Idempotent
        }
        
        String tokenHash = TokenHashUtil.hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void forgotPassword(AuthDto.ForgotPasswordRequest request) {
        List<User> users = userRepository.findByEmail(request.getEmail().toLowerCase());
        
        User user = null;
        if (users.size() == 1) {
            user = users.get(0);
        } else if (users.size() > 1 && StringUtils.hasText(request.getCollegeCode())) {
            user = users.stream()
                    .filter(u -> u.getCollege() != null && u.getCollege().getCollegeCode().equals(request.getCollegeCode()))
                    .findFirst()
                    .orElse(null);
        }

        if (user != null && user.getIsActive()) {
            String rawToken = TokenGeneratorUtil.generateSecureToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHashUtil.hashToken(rawToken));
            token.setExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour expiry
            passwordResetTokenRepository.save(token);
            
            // In a real app, send email with rawToken here.
        }
        // Always return success immediately to avoid enumeration.
    }

    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        String tokenHash = TokenHashUtil.hashToken(request.getToken());
        PasswordResetToken storedToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        // Atomic consumption
        int affected = passwordResetTokenRepository.consume(storedToken.getId());
        if (affected == 0) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        User user = storedToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens for this user
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Transactional
    public void resendVerification(AuthDto.ResendVerificationRequest request) {
        List<User> users = userRepository.findByEmail(request.getEmail().toLowerCase());
        
        User user = null;
        if (users.size() == 1) {
            user = users.get(0);
        } else if (users.size() > 1 && StringUtils.hasText(request.getCollegeCode())) {
            user = users.stream()
                    .filter(u -> u.getCollege() != null && u.getCollege().getCollegeCode().equals(request.getCollegeCode()))
                    .findFirst()
                    .orElse(null);
        }

        if (user != null && !user.getIsVerified()) {
            // Invalidate old tokens
            emailVerificationTokenRepository.invalidateAllForUser(user.getId());
            
            String rawToken = TokenGeneratorUtil.generateSecureToken();
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUser(user);
            token.setTokenHash(TokenHashUtil.hashToken(rawToken));
            token.setExpiresAt(Instant.now().plusSeconds(86400)); // 24 hours expiry
            emailVerificationTokenRepository.save(token);
            
            // In a real app, send email here.
        }
    }

    @Transactional
    public void verifyEmail(AuthDto.VerifyEmailRequest request) {
        String tokenHash = TokenHashUtil.hashToken(request.getToken());
        EmailVerificationToken storedToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        // Atomic consumption
        int affected = emailVerificationTokenRepository.consume(storedToken.getId());
        if (affected == 0) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        User user = storedToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);
    }

    public static class LoginResult {
        public final AuthDto.AuthResponse authResponse;
        public final String refreshToken;

        public LoginResult(AuthDto.AuthResponse authResponse, String refreshToken) {
            this.authResponse = authResponse;
            this.refreshToken = refreshToken;
        }
    }
}
