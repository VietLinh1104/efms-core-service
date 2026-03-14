package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {
    List<InvoiceLine> findByInvoiceIdOrderByIdAsc(UUID invoiceId);
}
