package com.rentalcar.service;

import com.rentalcar.dto.request.LoginRequest;
import com.rentalcar.dto.request.RegisterRequest;
import com.rentalcar.dto.response.AuthResponse;
import com.rentalcar.entity.RefreshToken;
import com.rentalcar.entity.User;
import com.rentalcar.enums.Role;
import com.rentalcar.exception.InvalidTokenException;
import com.rentalcar.exception.ResourceAlreadyExistsException;
import com.rentalcar.exception.ResourceNotFoundException;
import com.rentalcar.repository.RefreshTokenRepository;
import com.rentalcar.repository.UserRepository;
import com.rentalcar.security.JwtTokenProvider;
import com.rentalcar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final AuthenticationManager  authenticationManager;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Register ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail().toLowerCase())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(Role.ROLE_USER)
            .enabled(true)
            .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(user).getToken();

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring Security validates credentials and throws on failure
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()
            )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUsername()));

        // Revoke any existing refresh token and issue a fresh one
        refreshTokenRepository.revokeAllUserTokens(user.getId());
        String accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = createRefreshToken(user).getToken();

        log.info("User logged in: {}", user.getUsername());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Refresh token ──────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = stored.getUser();
        UserPrincipal principal = UserPrincipal.from(user);

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken  = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = createRefreshToken(user).getToken();

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("User logged out: {}", userId);
    }

    // ── Change password ────────────────────────────────────────────────────

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidTokenException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all refresh tokens — force re-login everywhere
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("Password changed for user: {}", user.getUsername());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
            .revoked(false)
            .build();
        return refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpirationMs / 1000)
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
    }
}
