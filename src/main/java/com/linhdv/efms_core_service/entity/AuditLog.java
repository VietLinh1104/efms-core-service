package com.linhdv.efms_core_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs", schema = "public")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @NotNull
    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Size(max = 20)
    @NotNull
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    // UUID từ Identity Service — không có @ManyToOne/@JoinColumn
    @Column(name = "changed_by")
    private UUID changedBy;

    @ColumnDefault("now()")
    @Column(name = "changed_at")
    private Instant changedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data")
    private Map<String, Object> oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private Map<String, Object> newData;

}