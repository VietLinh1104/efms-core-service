package com.linhdv.efms_core_service.service.finance;

import com.linhdv.efms_core_service.dto.finance.request.CreateBankAccountRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankAccountResponse;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.BankAccount;
import com.linhdv.efms_core_service.repository.finance.BankAccountRepository;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public PagedResponse<BankAccountResponse> search(UUID companyId, String type, String search, int page, int size) {
        Page<BankAccount> data = bankAccountRepository.search(companyId, type, search, PageRequest.of(page, size));
        List<BankAccountResponse> content = data.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BankAccountResponse create(CreateBankAccountRequest req) {
        BankAccount ba = new BankAccount();
        ba.setCompanyId(req.getCompanyId());
        
        if (req.getGlAccountId() != null) {
            Account acc = new Account(); acc.setId(req.getGlAccountId());
            ba.setGlAccount(acc);
        }

        ba.setName(req.getName());
        ba.setBankName(req.getBankName());
        ba.setAccountNumber(req.getAccountNumber());
        ba.setType(req.getType());
        ba.setCurrencyCode(req.getCurrencyCode());
        ba.setOpeningBalance(req.getOpeningBalance());
        ba.setIsActive(true);
        ba.setCreatedAt(Instant.now());

        return toResponse(bankAccountRepository.save(ba));
    }

    @Transactional
    public BankAccountResponse update(UUID id, CreateBankAccountRequest req) {
        BankAccount ba = findOrThrow(id);

        if (req.getGlAccountId() != null) {
            Account acc = new Account(); acc.setId(req.getGlAccountId());
            ba.setGlAccount(acc);
        } else {
            ba.setGlAccount(null);
        }

        ba.setName(req.getName());
        ba.setBankName(req.getBankName());
        ba.setAccountNumber(req.getAccountNumber());
        ba.setType(req.getType());
        ba.setCurrencyCode(req.getCurrencyCode());
        ba.setOpeningBalance(req.getOpeningBalance());

        return toResponse(bankAccountRepository.save(ba));
    }

    @Transactional
    public BankAccountResponse toggleActive(UUID id) {
        BankAccount ba = findOrThrow(id);
        ba.setIsActive(!ba.getIsActive());
        return toResponse(bankAccountRepository.save(ba));
    }

    private BankAccount findOrThrow(UUID id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản ngân hàng không tồn tại: " + id));
    }

    private BankAccountResponse toResponse(BankAccount ba) {
        return BankAccountResponse.builder()
                .id(ba.getId())
                .name(ba.getName())
                .bankName(ba.getBankName())
                .accountNumber(ba.getAccountNumber())
                .type(ba.getType())
                .currencyCode(ba.getCurrencyCode())
                .openingBalance(ba.getOpeningBalance())
                .isActive(ba.getIsActive())
                .glAccountId(ba.getGlAccount() != null ? ba.getGlAccount().getId() : null)
                .glAccountCode(ba.getGlAccount() != null ? ba.getGlAccount().getCode() : null)
                .createdAt(ba.getCreatedAt())
                .build();
    }
}
