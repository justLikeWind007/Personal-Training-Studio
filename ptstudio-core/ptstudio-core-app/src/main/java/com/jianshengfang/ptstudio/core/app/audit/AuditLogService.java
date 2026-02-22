package com.jianshengfang.ptstudio.core.app.audit;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AuditLogService {

    private final List<AuditLogEntry> logs = Collections.synchronizedList(new ArrayList<>());

    public void log(String module, String action, String detail, String operator) {
        logs.add(new AuditLogEntry(module, action, detail, operator, OffsetDateTime.now()));
    }

    public List<AuditLogEntry> list() {
        return List.copyOf(logs);
    }

    public record AuditLogEntry(String module,
                                String action,
                                String detail,
                                String operator,
                                OffsetDateTime occurredAt) {
    }
}
