package com.linhdv.efms_core_service.repository.invoice;

import com.linhdv.efms_core_service.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {
    List<InvoicePayment> findByInvoiceIdOrderByIdAsc(UUID invoiceId);
    List<InvoicePayment> findByPaymentIdOrderByIdAsc(UUID paymentId);
}
