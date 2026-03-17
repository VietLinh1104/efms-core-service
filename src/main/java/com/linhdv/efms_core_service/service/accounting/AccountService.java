package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateAccountRequest;
import com.linhdv.efms_core_service.dto.accounting.response.AccountBalanceResponse;
import com.linhdv.efms_core_service.dto.accounting.response.AccountResponse;
import com.linhdv.efms_core_service.repository.accounting.AccountRepository;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.Company;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    // ── Danh sách ──────────────────────────────────────────────────────────

    /** Trả về danh sách phẳng tất cả tài khoản */
    @Transactional(readOnly = true)
    public List<AccountResponse> listAll(UUID companyId) {
        return accountRepository.findByCompanyIdOrderByCode(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Trả về danh sách phẳng tất cả tài khoản */
    @Transactional(readOnly = true)
    public List<AccountResponse> listAllPage(UUID companyId) {
        return accountRepository.findByCompanyIdOrderByCode(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Trả về danh sách dạng cây (chỉ root, children được nhúng vào) */
    @Transactional(readOnly = true)
    public List<AccountResponse> listTree(UUID companyId) {
        List<Account> all = accountRepository.findByCompanyIdOrderByCode(companyId);
        Map<UUID, AccountResponse> map = all.stream()
                .collect(Collectors.toMap(Account::getId, this::toResponse));

        // Gán children
        all.forEach(a -> {
            if (a.getParent() != null) {
                AccountResponse parent = map.get(a.getParent().getId());
                if (parent != null)
                    parent.getChildren().add(map.get(a.getId()));
            }
        });

        // Chỉ trả root
        return all.stream()
                .filter(a -> a.getParent() == null)
                .map(a -> map.get(a.getId()))
                .toList();
    }

    // ── Chi tiết ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // ── Tạo mới ────────────────────────────────────────────────────────────

    @Transactional
    public AccountResponse create(CreateAccountRequest req) {
        Company company = new Company();
        company.setId(req.getCompanyId());

        Account account = new Account();
        account.setCompany(company);
        account.setCode(req.getCode());
        account.setName(req.getName());
        account.setType(req.getType());
        account.setBalanceType(req.getBalanceType());
        account.setIsActive(true);
        account.setCreatedAt(Instant.now());

        if (req.getParentId() != null) {
            Account parent = findOrThrow(req.getParentId());
            account.setParent(parent);
        }

        return toResponse(accountRepository.save(account));
    }

    // ── Cập nhật ───────────────────────────────────────────────────────────

    @Transactional
    public AccountResponse update(UUID id, CreateAccountRequest req) {
        Account account = findOrThrow(id);
        account.setCode(req.getCode());
        account.setName(req.getName());
        account.setType(req.getType());
        account.setBalanceType(req.getBalanceType());

        if (req.getParentId() != null) {
            account.setParent(findOrThrow(req.getParentId()));
        } else {
            account.setParent(null);
        }

        return toResponse(accountRepository.save(account));
    }

    // ── Bật / tắt ──────────────────────────────────────────────────────────

    @Transactional
    public AccountResponse toggleActive(UUID id) {
        Account account = findOrThrow(id);
        account.setIsActive(!account.getIsActive());
        return toResponse(accountRepository.save(account));
    }

    // ── Số dư ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(UUID id, LocalDate fromDate, LocalDate toDate) {
        Account account = findOrThrow(id);
        // TODO: tổng hợp số dư từ JournalLineRepository
        return AccountBalanceResponse.builder()
                .accountCode(account.getCode())
                .accountName(account.getName())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .openingBalance(BigDecimal.ZERO)
                .closingBalance(BigDecimal.ZERO)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Account findOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản không tồn tại: " + id));
    }

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .type(a.getType())
                .balanceType(a.getBalanceType())
                .isActive(a.getIsActive())
                .createdAt(a.getCreatedAt())
                .parentId(a.getParent() != null ? a.getParent().getId() : null)
                .parentName(a.getParent() != null ? a.getParent().getName() : null)
                .children(new ArrayList<>())
                .build();
    }
}
