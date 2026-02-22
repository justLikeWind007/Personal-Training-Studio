package com.jianshengfang.ptstudio.core.adapter.context;

import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantStoreContextInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String STORE_HEADER = "X-Store-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        String storeId = request.getHeader(STORE_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "tenant-demo";
        }
        if (storeId == null || storeId.isBlank()) {
            storeId = "store-001";
        }
        TenantStoreContextHolder.set(new TenantStoreContext(tenantId, storeId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantStoreContextHolder.clear();
    }
}
