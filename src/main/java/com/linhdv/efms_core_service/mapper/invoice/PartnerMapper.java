package com.linhdv.efms_core_service.mapper.invoice;

import com.linhdv.efms_core_service.dto.invoice.request.CreatePartnerRequest;
import com.linhdv.efms_core_service.dto.invoice.response.PartnerResponse;
import com.linhdv.efms_core_service.entity.Account;
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
public interface PartnerMapper {

    @Mapping(target = "arAccount", source = "arAccountId", qualifiedByName = "accountFromId")
    @Mapping(target = "apAccount", source = "apAccountId", qualifiedByName = "accountFromId")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Partner toEntity(CreatePartnerRequest request);

    @Mapping(target = "arAccountId", source = "arAccount.id")
    @Mapping(target = "arAccountCode", source = "arAccount.code")
    @Mapping(target = "apAccountId", source = "apAccount.id")
    @Mapping(target = "apAccountCode", source = "apAccount.code")
    PartnerResponse toResponse(Partner partner);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "arAccount", source = "arAccountId", qualifiedByName = "accountFromId")
    @Mapping(target = "apAccount", source = "apAccountId", qualifiedByName = "accountFromId")
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(CreatePartnerRequest request, @MappingTarget Partner partner);

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
