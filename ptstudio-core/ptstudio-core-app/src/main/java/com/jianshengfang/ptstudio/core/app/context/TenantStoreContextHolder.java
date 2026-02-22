package com.jianshengfang.ptstudio.core.app.context;

public final class TenantStoreContextHolder {

    private static final ThreadLocal<TenantStoreContext> CONTEXT = new ThreadLocal<>();

    private TenantStoreContextHolder() {
    }

    public static void set(TenantStoreContext context) {
        CONTEXT.set(context);
    }

    public static TenantStoreContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
