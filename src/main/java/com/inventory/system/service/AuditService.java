package com.inventory.system.service;

import com.inventory.system.entity.AuditLog;
import com.inventory.system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging business-critical actions into the audit log.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(String action, String entityType, Long entityId, String details, String performedBy) {
        String actor = performedBy != null ? performedBy : getCurrentUsername();

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .performedBy(actor)
                .build();

        auditLogRepository.save(auditLog);
        log.info("AUDIT: action='{}', entity='{}#{}', by='{}'", action, entityType, entityId, actor);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logAction(String action, String entityType, Long entityId, String details) {
        logAction(action, entityType, entityId, details, getCurrentUsername());
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
