package com.linhdv.efms_core_service.mapper.invoice;

import com.linhdv.efms_core_service.dto.invoice.request.InvoiceLineRequest;
import com.linhdv.efms_core_service.dto.invoice.request.InvoiceRequest;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceLineResponse;
import com.linhdv.efms_core_service.dto.invoice.response.InvoiceResponse;
import com.linhdv.efms_core_service.entity.Account;
import com.linhdv.efms_core_service.entity.Invoice;
import com.linhdv.efms_core_service.entity.InvoiceLine;
import com.linhdv.efms_core_service.entity.Partner;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(target = "partner", source = "partnerId", qualifiedByName = "partnerFromId")
    @Mapping(target = "status", constant = "draft")
    @Mapping(target = "currencyCode", source = "currencyCode", defaultValue = "VND")
    @Mapping(target = "exchangeRate", source = "exchangeRate", defaultExpression = "java(java.math.BigDecimal.ONE)")
    @Mapping(target = "subtotal", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "taxAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "totalAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "paidAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Invoice toEntity(InvoiceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "partner", source = "partnerId", qualifiedByName = "partnerFromId")
    @Mapping(target = "invoiceType", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvalComment", ignore = true)
    @Mapping(target = "journalEntry", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    void updateEntityFromRequest(InvoiceRequest request, @MappingTarget Invoice invoice);

    @Mapping(target = "partnerId", source = "partner.id")
    @Mapping(target = "partnerName", source = "partner.name")
    @Mapping(target = "journalEntryId", source = "journalEntry.id")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountFromId")
    InvoiceLine toLineEntity(InvoiceLineRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", source = "accountId", qualifiedByName = "accountFromId")
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    void updateLineFromRequest(InvoiceLineRequest request, @MappingTarget InvoiceLine line);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountName", source = "account.name")
    InvoiceLineResponse toLineResponse(InvoiceLine line);

    @Named("partnerFromId")
    default Partner partnerFromId(UUID partnerId) {
        if (partnerId == null) {
            return null;
        }
        Partner partner = new Partner();
        partner.setId(partnerId);
        return partner;
    }

    @Named("accountFromId")
    default Account accountFromId(UUID accountId) {
        if (accountId == null) {
            return null;
        }
        Account account = new Account();
        account.setId(accountId);
        return account;
    }
}
