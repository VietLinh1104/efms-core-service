package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.TrialBalanceLineResponse;
import com.linhdv.efms_core_service.dto.accounting.response.TrialBalanceResponse;
import com.linhdv.efms_core_service.repository.accounting.AccountRepository;
import com.linhdv.efms_core_service.repository.accounting.FiscalPeriodRepository;
import com.linhdv.efms_core_service.repository.accounting.JournalLineRepository;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.FiscalPeriod;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrialBalanceService {

    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    @Transactional(readOnly = true)
    public TrialBalanceResponse getByPeriod(UUID companyId, UUID periodId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new EntityNotFoundException("Kỳ kế toán không tồn tại: " + periodId));
        return build(companyId, period.getId(), period.getName(), period.getStartDate(), period.getEndDate());
    }

    @Transactional(readOnly = true)
    public TrialBalanceResponse getByDateRange(UUID companyId, LocalDate fromDate, LocalDate toDate) {
        return build(companyId, null, null, fromDate, toDate);
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private TrialBalanceResponse build(UUID companyId, UUID periodId, String periodName,
            LocalDate fromDate, LocalDate toDate) {
        // Lấy tất cả tài khoản
        Map<UUID, Account> accounts = accountRepository.findByCompanyIdOrderByCode(companyId)
                .stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        // Lấy phát sinh trong kỳ
        List<Object[]> aggregated = journalLineRepository.aggregateByAccount(companyId, fromDate, toDate);

        List<TrialBalanceLineResponse> lines = aggregated.stream().map(row -> {
            UUID accountId = (UUID) row[0];
            BigDecimal debit = (BigDecimal) row[1];
            BigDecimal credit = (BigDecimal) row[2];
            Account acc = accounts.get(accountId);
            String code = acc != null ? acc.getCode() : accountId.toString();
            String name = acc != null ? acc.getName() : "";

            boolean isDebitNature = acc != null && "debit".equalsIgnoreCase(acc.getBalanceType());
            BigDecimal closing = isDebitNature
                    ? debit.subtract(credit)
                    : credit.subtract(debit);

            return TrialBalanceLineResponse.builder()
                    .accountCode(code)
                    .accountName(name)
                    .openingDebit(BigDecimal.ZERO) // TODO: tính số dư đầu kỳ
                    .openingCredit(BigDecimal.ZERO)
                    .periodDebit(debit)
                    .periodCredit(credit)
                    .closingDebit(closing.compareTo(BigDecimal.ZERO) > 0 ? closing : BigDecimal.ZERO)
                    .closingCredit(closing.compareTo(BigDecimal.ZERO) < 0 ? closing.abs() : BigDecimal.ZERO)
                    .build();
        }).toList();

        return TrialBalanceResponse.builder()
                .periodId(periodId)
                .periodName(periodName)
                .fromDate(fromDate)
                .toDate(toDate)
                .lines(lines)
                .build();
    }
}
