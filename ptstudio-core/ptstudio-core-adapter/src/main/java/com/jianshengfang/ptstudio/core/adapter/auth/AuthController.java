package com.jianshengfang.ptstudio.core.adapter.auth;

import com.jianshengfang.ptstudio.core.app.auth.AuthService;
import com.jianshengfang.ptstudio.core.app.auth.UserIdentity;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.mobile(), request.password());
        return LoginResponse.from(result.token(), result.identity(), TenantStoreContextHolder.get());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        UserIdentity identity = authService.currentUser(extractToken(authorization))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期"));
        return ProfileResponse.from(identity, TenantStoreContextHolder.get());
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return authorization;
    }

    public record LoginRequest(@NotBlank String mobile, @NotBlank String password) {
    }

    public record LoginResponse(String token,
                                Long userId,
                                String username,
                                String mobile,
                                String tenantId,
                                String storeId,
                                Object roles) {
        static LoginResponse from(String token, UserIdentity identity, TenantStoreContext context) {
            return new LoginResponse(
                    token,
                    identity.userId(),
                    identity.username(),
                    identity.mobile(),
                    context != null ? context.tenantId() : identity.tenantId(),
                    context != null ? context.storeId() : identity.storeId(),
                    identity.roles()
            );
        }
    }

    public record ProfileResponse(Long userId,
                                  String username,
                                  String mobile,
                                  String tenantId,
                                  String storeId,
                                  Object roles) {
        static ProfileResponse from(UserIdentity identity, TenantStoreContext context) {
            return new ProfileResponse(
                    identity.userId(),
                    identity.username(),
                    identity.mobile(),
                    context != null ? context.tenantId() : identity.tenantId(),
                    context != null ? context.storeId() : identity.storeId(),
                    identity.roles()
            );
        }
    }
}
