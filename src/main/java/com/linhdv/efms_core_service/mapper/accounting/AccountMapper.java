package com.linhdv.efms_core_service.mapper.accounting;

import com.linhdv.efms_core_service.dto.accounting.request.CreateAccountRequest;
import com.linhdv.efms_core_service.dto.accounting.response.AccountResponse;
import com.linhdv.efms_core_service.entity.Account;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Account toEntity(CreateAccountRequest request);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", expression = "java(new java.util.ArrayList<>())")
    AccountResponse toResponse(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(CreateAccountRequest request, @MappingTarget Account account);
}
