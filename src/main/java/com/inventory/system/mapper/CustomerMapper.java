package com.inventory.system.mapper;

import com.inventory.system.dto.request.CustomerRequest;
import com.inventory.system.dto.response.CustomerResponse;
import com.inventory.system.entity.Customer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerRequest request);

    CustomerResponse toResponse(Customer customer);

    List<CustomerResponse> toResponseList(List<Customer> customers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
