package com.linhdv.efms_core_service.service.invoice;

import com.linhdv.efms_core_service.dto.invoice.request.CreatePartnerRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PartnerResponse;
import com.linhdv.efms_core_service.mapper.invoice.PartnerMapper;
import com.linhdv.efms_core_service.repository.invoice.PartnerRepository;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.Partner;
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
public class PartnerService {

    private static final String TABLE = "partners";

    private final PartnerRepository partnerRepository;
    private final AuditService auditService;
    private final PartnerMapper partnerMapper;

    @Transactional(readOnly = true)
    public PagedResponse<PartnerResponse> search(UUID companyId, String type, String keyword, int page, int size) {
        // Đảm bảo keyword là null nếu nó rỗng hoặc chỉ toàn khoảng trắng
        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        Page<Partner> data = partnerRepository.search(companyId, type, searchKeyword, PageRequest.of(page, size));
        List<PartnerResponse> content = data.getContent().stream().map(partnerMapper::toResponse).toList();
        return PagedResponse.of(content, page, size, data.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PartnerResponse getById(UUID id) {
        return partnerMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public PartnerResponse create(CreatePartnerRequest req) {
        Partner p = partnerMapper.toEntity(req);

        Partner result = partnerRepository.save(p);
        auditService.log(TABLE, result.getId(), "INSERT", null, auditService.toMap(result));
        return partnerMapper.toResponse(result);
    }

    @Transactional
    public PartnerResponse update(UUID id, CreatePartnerRequest req) {
        Partner old = findOrThrow(id);
        Map<String, Object> oldSnapshot = auditService.toMap(old);

        partnerMapper.updateEntityFromRequest(req, old);

        Partner result = partnerRepository.save(old);
        auditService.log(TABLE, id, "UPDATE", oldSnapshot, auditService.toMap(result));
        return partnerMapper.toResponse(result);
    }

    @Transactional
    public PartnerResponse toggleActive(UUID id) {
        Partner p = findOrThrow(id);
        Map<String, Object> oldSnapshot = auditService.toMap(p);
        p.setIsActive(!p.getIsActive());
        Partner result = partnerRepository.save(p);
        auditService.log(TABLE, id, "UPDATE", oldSnapshot, auditService.toMap(result));
        return partnerMapper.toResponse(result);
    }

    private Partner findOrThrow(UUID id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đối tác: " + id));
    }
}
