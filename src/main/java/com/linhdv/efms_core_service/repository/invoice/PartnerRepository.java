package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.Partner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    @Query("""
        SELECT p FROM Partner p
        WHERE p.company.id = :companyId
          AND (:type IS NULL OR p.type = :type)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY p.createdAt DESC
        """)
    Page<Partner> search(
            @Param("companyId") UUID companyId,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable
    );
}
