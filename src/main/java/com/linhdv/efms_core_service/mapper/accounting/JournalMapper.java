package com.linhdv.efms_core_service.mapper.accounting;

import com.linhdv.efms_core_service.dto.accounting.response.JournalEntryResponse;
import com.linhdv.efms_core_service.dto.accounting.response.JournalLineResponse;
import com.linhdv.efms_core_service.entity.JournalEntry;
import com.linhdv.efms_core_service.entity.JournalLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JournalMapper {

    JournalEntryResponse toResponse(JournalEntry journalEntry);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "partnerId", source = "partner.id")
    @Mapping(target = "partnerName", source = "partner.name")
    JournalLineResponse toLineResponse(JournalLine journalLine);
}
