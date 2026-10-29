package com.inventory.system.mapper;

import com.inventory.system.dto.response.StockMovementResponse;
import com.inventory.system.entity.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    StockMovementResponse toResponse(StockMovement stockMovement);

    List<StockMovementResponse> toResponseList(List<StockMovement> stockMovements);
}
