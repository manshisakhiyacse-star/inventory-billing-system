package com.inventory.system.dto;

import com.inventory.system.dto.request.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("ProductRequest should pass validation with valid data")
    void testValidProductRequest() {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-1001")
                .name("Wireless Mouse")
                .description("Ergonomic optical mouse")
                .category("Electronics")
                .unitPrice(new BigDecimal("29.99"))
                .costPrice(new BigDecimal("15.00"))
                .quantityInStock(50)
                .reorderThreshold(10)
                .build();

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations for valid product");
    }

    @Test
    @DisplayName("ProductRequest should fail on blank SKU and negative price")
    void testInvalidProductRequest() {
        ProductRequest request = ProductRequest.builder()
                .sku("")
                .name("")
                .unitPrice(new BigDecimal("-5.00"))
                .costPrice(new BigDecimal("-1.00"))
                .quantityInStock(-10)
                .reorderThreshold(-2)
                .build();

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Should violate constraints for blank name and negative values");
    }

    @Test
    @DisplayName("CustomerRequest should validate email format")
    void testCustomerEmailValidation() {
        CustomerRequest invalidCustomer = CustomerRequest.builder()
                .name("Test Client")
                .email("not-a-valid-email")
                .build();

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(invalidCustomer);
        assertFalse(violations.isEmpty(), "Should catch invalid email");

        CustomerRequest validCustomer = CustomerRequest.builder()
                .name("Test Client")
                .email("client@example.com")
                .build();

        violations = validator.validate(validCustomer);
        assertTrue(violations.isEmpty(), "Should accept valid email");
    }

    @Test
    @DisplayName("InvoiceRequest should require customerId and non-empty lineItems")
    void testInvoiceRequestValidation() {
        InvoiceRequest invalidInvoice = InvoiceRequest.builder()
                .customerId(null)
                .lineItems(Collections.emptyList())
                .taxRate(new BigDecimal("1.5000")) // Exceeds 100%
                .build();

        Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(invalidInvoice);
        assertFalse(violations.isEmpty(), "Should fail with empty items and excessive tax");

        InvoiceRequest validInvoice = InvoiceRequest.builder()
                .customerId(1L)
                .lineItems(List.of(
                        InvoiceLineItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .taxRate(new BigDecimal("0.1800"))
                .discountAmount(new BigDecimal("5.00"))
                .build();

        violations = validator.validate(validInvoice);
        assertTrue(violations.isEmpty(), "Should pass for valid invoice request");
    }

    @Test
    @DisplayName("LoginRequest should require non-blank username and password")
    void testLoginRequestValidation() {
        LoginRequest blankLogin = new LoginRequest("", "");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(blankLogin);
        assertFalse(violations.isEmpty());

        LoginRequest validLogin = new LoginRequest("admin", "admin123");
        violations = validator.validate(validLogin);
        assertTrue(violations.isEmpty());
    }
}
