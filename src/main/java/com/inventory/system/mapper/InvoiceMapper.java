package com.inventory.system.mapper;

import com.inventory.system.dto.response.InvoiceLineItemResponse;
import com.inventory.system.dto.response.InvoiceResponse;
import com.inventory.system.entity.Invoice;
import com.inventory.system.entity.InvoiceLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    @Mapping(target = "productId", source = "product.id")
    InvoiceLineItemResponse toLineItemResponse(InvoiceLineItem lineItem);

    List<InvoiceLineItemResponse> toLineItemResponseList(List<InvoiceLineItem> lineItems);
}
