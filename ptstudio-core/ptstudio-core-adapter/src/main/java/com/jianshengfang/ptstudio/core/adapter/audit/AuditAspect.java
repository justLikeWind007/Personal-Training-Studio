package com.jianshengfang.ptstudio.core.adapter.audit;

import com.jianshengfang.ptstudio.core.app.audit.AuditLogService;
import com.jianshengfang.ptstudio.core.app.auth.AuthService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final AuthService authService;

    public AuditAspect(AuditLogService auditLogService, AuthService authService) {
        this.auditLogService = auditLogService;
        this.authService = authService;
    }

    @AfterReturning("@annotation(com.jianshengfang.ptstudio.core.adapter.audit.AuditAction)")
    public void afterReturning(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditAction annotation = signature.getMethod().getAnnotation(AuditAction.class);

        String operator = "anonymous";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String authorization = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                String token = authorization.startsWith("Bearer ")
                        ? authorization.substring("Bearer ".length())
                        : authorization;
                operator = authService.currentUser(token)
                        .map(user -> user.username() + "(" + user.userId() + ")")
                        .orElse(operator);
            }
        }

        auditLogService.log(
                annotation.module(),
                annotation.action(),
                signature.getDeclaringType().getSimpleName() + "." + signature.getName(),
                operator
        );
    }
}
