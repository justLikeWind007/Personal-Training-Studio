package com.jianshengfang.ptstudio.core.adapter.context;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.settings.StoreSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantStoreContextInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String STORE_HEADER = "X-Store-Id";

    private final StoreSettingsService storeSettingsService;

    public TenantStoreContextInterceptor(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        String storeId = request.getHeader(STORE_HEADER);
        if (tenantId == null || tenantId.isBlank() || storeId == null || storeId.isBlank()) {
            throw new IllegalArgumentException("缺少租户门店上下文请求头");
        }
        if (isWriteMethod(request.getMethod()) && shouldValidateWritableStore(request.getRequestURI())
                && !storeSettingsService.isStoreWritable(tenantId, storeId)) {
            throw new IllegalArgumentException("门店已停用，禁止写操作");
        }
        TenantStoreContextHolder.set(new TenantStoreContext(tenantId, storeId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantStoreContextHolder.clear();
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean shouldValidateWritableStore(String uri) {
        if (uri == null) {
            return true;
        }
        return !uri.startsWith("/api/admin/stores")
                && !uri.startsWith("/api/auth/");
    }
}
