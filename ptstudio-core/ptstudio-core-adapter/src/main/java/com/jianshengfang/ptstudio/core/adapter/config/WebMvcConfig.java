package com.jianshengfang.ptstudio.core.adapter.config;

import com.jianshengfang.ptstudio.core.adapter.context.TenantStoreContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantStoreContextInterceptor tenantStoreContextInterceptor;

    public WebMvcConfig(TenantStoreContextInterceptor tenantStoreContextInterceptor) {
        this.tenantStoreContextInterceptor = tenantStoreContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantStoreContextInterceptor).addPathPatterns("/api/**");
    }
}
