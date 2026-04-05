package com.linhdv.efms_core_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "invoices", schema = "public")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // UUID từ Identity Service — không có @ManyToOne/@JoinColumn
    @NotNull
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    // @ManyToOne nội bộ Core DB
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Size(max = 5)
    @NotNull
    @Column(name = "invoice_type", nullable = false, length = 5)
    private String invoiceType;

    @Size(max = 100)
    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @NotNull
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Size(max = 3)
    @NotNull
    @ColumnDefault("'VND'")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @ColumnDefault("1")
    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'draft'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 20)
    @Column(name = "approval_status", length = 20)
    private String approvalStatus;

    @Column(name = "approval_comment", length = Integer.MAX_VALUE)
    private String approvalComment;

    @Size(max = 255)
    @Column(name = "camunda_process_id", length = 255)
    private String camundaProcessId;

    // @ManyToOne nội bộ Core DB
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    // UUID từ Identity Service
    @Column(name = "created_by")
    private UUID createdBy;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

}