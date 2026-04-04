package com.linhdv.efms_core_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "fiscal_periods", schema = "public")
public class FiscalPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // UUID từ Identity Service — không có @ManyToOne/@JoinColumn
    @NotNull
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Size(max = 50)
    @NotNull
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'open'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // UUID từ Identity Service — user đã đóng kỳ kế toán
    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

}