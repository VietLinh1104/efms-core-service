package com.linhdv.efms_core_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "partners", schema = "public")
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // UUID từ Identity Service — không có @ManyToOne/@JoinColumn
    @NotNull
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 20)
    @NotNull
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Size(max = 50)
    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Size(max = 50)
    @Column(name = "phone", length = 50)
    private String phone;

    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @Column(name = "address", length = Integer.MAX_VALUE)
    private String address;

    // @ManyToOne nội bộ Core DB — accounts vẫn cùng schema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_account_id")
    private Account arAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ap_account_id")
    private Account apAccount;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

}