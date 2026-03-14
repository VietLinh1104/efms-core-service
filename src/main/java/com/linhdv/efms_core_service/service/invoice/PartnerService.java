package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.dto.invoice.request.CreatePartnerRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PartnerResponse;
import com.linhdv.efms_core_service.repository.invoice.PartnerRepository;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.Company;
import com.linhdv.efms_core_service.entity.Partner;
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
public class PartnerService {

    private final PartnerRepository partnerRepository;

    @Transactional(readOnly = true)
    public PagedResponse<PartnerResponse> search(UUID companyId, String type, String keyword, int page, int size) {
        Page<Partner> data = partnerRepository.search(companyId, type, keyword, PageRequest.of(page, size));
        List<PartnerResponse> content = data.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PartnerResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public PartnerResponse create(CreatePartnerRequest req) {
        Company company = new Company();
        company.setId(req.getCompanyId());

        Account ar = new Account(); ar.setId(req.getArAccountId());
        Account ap = new Account(); ap.setId(req.getApAccountId());

        Partner p = new Partner();
        p.setCompany(company);
        p.setName(req.getName());
        p.setType(req.getType());
        p.setTaxCode(req.getTaxCode());
        p.setPhone(req.getPhone());
        p.setEmail(req.getEmail());
        p.setAddress(req.getAddress());
        p.setArAccount(ar);
        p.setApAccount(ap);
        p.setIsActive(true);
        p.setCreatedAt(Instant.now());

        return toResponse(partnerRepository.save(p));
    }

    @Transactional
    public PartnerResponse update(UUID id, CreatePartnerRequest req) {
        Partner p = findOrThrow(id);
        
        Account ar = new Account(); ar.setId(req.getArAccountId());
        Account ap = new Account(); ap.setId(req.getApAccountId());

        p.setName(req.getName());
        p.setType(req.getType());
        p.setTaxCode(req.getTaxCode());
        p.setPhone(req.getPhone());
        p.setEmail(req.getEmail());
        p.setAddress(req.getAddress());
        p.setArAccount(ar);
        p.setApAccount(ap);

        return toResponse(partnerRepository.save(p));
    }

    @Transactional
    public PartnerResponse toggleActive(UUID id) {
        Partner p = findOrThrow(id);
        p.setIsActive(!p.getIsActive());
        return toResponse(partnerRepository.save(p));
    }

    private Partner findOrThrow(UUID id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đối tác: " + id));
    }

    private PartnerResponse toResponse(Partner p) {
        return PartnerResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType())
                .taxCode(p.getTaxCode())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .arAccountId(p.getArAccount() != null ? p.getArAccount().getId() : null)
                .arAccountCode(p.getArAccount() != null ? p.getArAccount().getCode() : null)
                .apAccountId(p.getApAccount() != null ? p.getApAccount().getId() : null)
                .apAccountCode(p.getApAccount() != null ? p.getApAccount().getCode() : null)
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
