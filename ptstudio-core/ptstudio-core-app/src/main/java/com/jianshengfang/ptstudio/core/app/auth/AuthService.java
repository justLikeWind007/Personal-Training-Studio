package com.jianshengfang.ptstudio.core.app.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final Map<String, UserIdentity> userByMobile = new ConcurrentHashMap<>();
    private final Map<String, String> passwordByMobile = new ConcurrentHashMap<>();
    private final Map<String, UserIdentity> sessionByToken = new ConcurrentHashMap<>();

    public AuthService() {
        UserIdentity sales = new UserIdentity(
                1001L,
                "sales.lee",
                "13800000001",
                "tenant-demo",
                "store-001",
                Set.of("SALES")
        );
        UserIdentity manager = new UserIdentity(
                1002L,
                "manager.wang",
                "13800000002",
                "tenant-demo",
                "store-001",
                Set.of("STORE_MANAGER", "FINANCE")
        );
        userByMobile.put(sales.mobile(), sales);
        userByMobile.put(manager.mobile(), manager);
        passwordByMobile.put(sales.mobile(), "123456");
        passwordByMobile.put(manager.mobile(), "123456");
    }

    public LoginResult login(String mobile, String password) {
        UserIdentity user = userByMobile.get(mobile);
        if (user == null || !Objects.equals(passwordByMobile.get(mobile), password)) {
            throw new IllegalArgumentException("手机号或密码错误");
        }
        String token = UUID.randomUUID().toString();
        sessionByToken.put(token, user);
        return new LoginResult(token, user);
    }

    public Optional<UserIdentity> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionByToken.get(token));
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessionByToken.remove(token);
    }

    public record LoginResult(String token, UserIdentity identity) {
    }
}
