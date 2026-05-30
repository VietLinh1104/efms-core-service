package com.linhdv.efms_core_service.mapper.finance;

import com.linhdv.efms_core_service.dto.finance.request.CreateBankAccountRequest;
import com.linhdv.efms_core_service.dto.finance.response.BankAccountResponse;
import com.linhdv.efms_core_service.entity.BankAccount;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BankAccountMapper {

    @Mapping(target = "glAccount", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    BankAccount toEntity(CreateBankAccountRequest request);

    @Mapping(target = "glAccountId", source = "glAccount.id")
    @Mapping(target = "glAccountCode", source = "glAccount.code")
    BankAccountResponse toResponse(BankAccount bankAccount);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "glAccount", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(CreateBankAccountRequest request, @MappingTarget BankAccount bankAccount);
}
