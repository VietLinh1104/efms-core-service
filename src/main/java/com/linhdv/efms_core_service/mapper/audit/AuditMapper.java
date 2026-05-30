package com.linhdv.efms_core_service.mapper.audit;

import com.linhdv.efms_core_service.dto.audit.response.AuditLogResponse;
import com.linhdv.efms_core_service.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
