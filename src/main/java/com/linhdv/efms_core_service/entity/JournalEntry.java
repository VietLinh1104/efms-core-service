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
@Table(name = "journal_entries", schema = "public")
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // UUID từ Identity Service — không có @ManyToOne/@JoinColumn
    @NotNull
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    // @ManyToOne nội bộ Core DB — fiscal_periods cùng schema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private FiscalPeriod period;

    @NotNull
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Size(max = 255)
    @Column(name = "reference")
    private String reference;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'draft'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 50)
    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    // UUID từ Identity Service
    @Column(name = "created_by")
    private UUID createdBy;

    // UUID từ Identity Service
    @Column(name = "posted_by")
    private UUID postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

}