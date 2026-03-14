package com.linhdv.efms_core_service.service.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateFiscalPeriodRequest;
import com.linhdv.efms_core_service.dto.accounting.response.FiscalPeriodResponse;
import com.linhdv.efms_core_service.repository.accounting.FiscalPeriodRepository;
import com.linhdv.efms_core_service.entity.Company;
import com.linhdv.efms_core_service.entity.FiscalPeriod;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FiscalPeriodService {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    @Transactional(readOnly = true)
    public List<FiscalPeriodResponse> list(UUID companyId) {
        return fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public FiscalPeriodResponse create(CreateFiscalPeriodRequest req) {
        Company company = new Company();
        company.setId(req.getCompanyId());

        FiscalPeriod period = new FiscalPeriod();
        period.setCompany(company);
        period.setName(req.getName());
        period.setStartDate(req.getStartDate());
        period.setEndDate(req.getEndDate());
        period.setStatus("open");
        period.setCreatedAt(Instant.now());

        return toResponse(fiscalPeriodRepository.save(period));
    }

    @Transactional
    public FiscalPeriodResponse close(UUID id) {
        FiscalPeriod period = findOrThrow(id);
        if ("closed".equals(period.getStatus())) {
            throw new IllegalStateException("Kỳ kế toán đã được đóng");
        }
        period.setStatus("closed");
        period.setClosedAt(Instant.now());
        return toResponse(fiscalPeriodRepository.save(period));
    }

    @Transactional
    public FiscalPeriodResponse reopen(UUID id) {
        FiscalPeriod period = findOrThrow(id);
        if ("open".equals(period.getStatus())) {
            throw new IllegalStateException("Kỳ kế toán đang ở trạng thái mở");
        }
        period.setStatus("open");
        period.setClosedAt(null);
        period.setClosedBy(null);
        return toResponse(fiscalPeriodRepository.save(period));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private FiscalPeriod findOrThrow(UUID id) {
        return fiscalPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Kỳ kế toán không tồn tại: " + id));
    }

    private FiscalPeriodResponse toResponse(FiscalPeriod p) {
        return FiscalPeriodResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .closedBy(p.getClosedBy() != null ? p.getClosedBy().getName() : null)
                .closedAt(p.getClosedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
