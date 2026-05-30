package com.linhdv.efms_core_service.service.finance;

import com.linhdv.efms_core_service.dto.finance.request.CreateBankAccountRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankAccountResponse;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.BankAccount;
import com.linhdv.efms_core_service.mapper.finance.BankAccountMapper;
import com.linhdv.efms_core_service.repository.finance.BankAccountRepository;
import com.linhdv.efms_core_service.service.AuditService;
import com.linhdv.efms_core_service.wrapper.PagedResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final String TABLE = "bank_accounts";

    private final BankAccountRepository bankAccountRepository;
    private final AuditService auditService;
    private final BankAccountMapper bankAccountMapper;

    @Transactional(readOnly = true)
    public PagedResponse<BankAccountResponse> search(UUID companyId, String type, String search, int page, int size) {
        Page<BankAccount> data = bankAccountRepository.search(companyId, type, search, PageRequest.of(page, size));
        List<BankAccountResponse> content = data.getContent().stream().map(bankAccountMapper::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getById(UUID id) {
        return bankAccountMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public BankAccountResponse create(CreateBankAccountRequest req) {
        BankAccount ba = bankAccountMapper.toEntity(req);
        
        if (req.getGlAccountId() != null) {
            Account acc = new Account(); acc.setId(req.getGlAccountId());
            ba.setGlAccount(acc);
        }

        BankAccount result = bankAccountRepository.save(ba);
        auditService.log(TABLE, result.getId(), "INSERT", null, auditService.toMap(result));
        return bankAccountMapper.toResponse(result);
    }

    @Transactional
    public BankAccountResponse update(UUID id, CreateBankAccountRequest req) {
        BankAccount ba = findOrThrow(id);

        Map<String, Object> oldSnapshot = auditService.toMap(ba);

        bankAccountMapper.updateEntityFromRequest(req, ba);

        if (req.getGlAccountId() != null) {
            Account acc = new Account(); acc.setId(req.getGlAccountId());
            ba.setGlAccount(acc);
        } else {
            ba.setGlAccount(null);
        }

        BankAccount result = bankAccountRepository.save(ba);
        auditService.log(TABLE, id, "UPDATE", oldSnapshot, auditService.toMap(result));
        return bankAccountMapper.toResponse(result);
    }

    @Transactional
    public BankAccountResponse toggleActive(UUID id) {
        BankAccount ba = findOrThrow(id);
        Map<String, Object> oldSnapshot = auditService.toMap(ba);
        ba.setIsActive(!ba.getIsActive());
        BankAccount result = bankAccountRepository.save(ba);
        auditService.log(TABLE, id, "UPDATE", oldSnapshot, auditService.toMap(result));
        return bankAccountMapper.toResponse(result);
    }

    private BankAccount findOrThrow(UUID id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tài khoản ngân hàng không tồn tại: " + id));
    }
}
