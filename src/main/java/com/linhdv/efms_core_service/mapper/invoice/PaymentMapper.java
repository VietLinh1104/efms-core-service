package com.linhdv.efms_core_service.mapper.invoice;

import com.linhdv.efms_core_service.dto.invoice.request.CreatePaymentRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PaymentResponse;
import com.linhdv.efms_core_service.entity.BankAccount;
import com.linhdv.efms_core_service.entity.Invoice;
import com.linhdv.efms_core_service.entity.Partner;
import com.linhdv.efms_core_service.entity.Payment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "partner", source = "partnerId", qualifiedByName = "partnerFromId")
    @Mapping(target = "bankAccount", source = "bankAccountId", qualifiedByName = "bankAccountFromId")
    @Mapping(target = "invoice", source = "invoiceId", qualifiedByName = "invoiceFromId")
    @Mapping(target = "currencyCode", source = "currencyCode", defaultValue = "VND")
    @Mapping(target = "exchangeRate", source = "exchangeRate", defaultExpression = "java(java.math.BigDecimal.ONE)")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Payment toEntity(CreatePaymentRequest request);

    @Mapping(target = "partnerId", source = "partner.id")
    @Mapping(target = "partnerName", source = "partner.name")
    @Mapping(target = "journalEntryId", source = "journalEntry.id")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "bankAccountId", source = "bankAccount.id")
    @Mapping(target = "bankAccountName", source = "bankAccount.name")
    PaymentResponse toResponse(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "partner", source = "partnerId", qualifiedByName = "partnerFromId")
    @Mapping(target = "bankAccount", source = "bankAccountId", qualifiedByName = "bankAccountFromId")
    @Mapping(target = "invoice", source = "invoiceId", qualifiedByName = "invoiceFromId")
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "journalEntry", ignore = true)
    void updateEntityFromRequest(CreatePaymentRequest request, @MappingTarget Payment payment);

    @Named("partnerFromId")
    default Partner partnerFromId(UUID partnerId) {
        if (partnerId == null) {
            return null;
        }
        Partner partner = new Partner();
        partner.setId(partnerId);
        return partner;
    }

    @Named("bankAccountFromId")
    default BankAccount bankAccountFromId(UUID bankAccountId) {
        if (bankAccountId == null) {
            return null;
        }
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(bankAccountId);
        return bankAccount;
    }

    @Named("invoiceFromId")
    default Invoice invoiceFromId(UUID invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        return invoice;
    }
}
