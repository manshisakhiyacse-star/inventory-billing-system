package com.inventory.system.entity;

import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.entity.enums.PaymentMethod;
import com.inventory.system.entity.enums.PaymentStatus;
import com.inventory.system.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    @DisplayName("Invoice addLineItem maintains bidirectional relationship")
    void testInvoiceAddLineItemBidirectional() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2026-0001")
                .status(InvoiceStatus.CREATED)
                .paymentMethod(PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PENDING)
                .createdBy("admin")
                .build();

        InvoiceLineItem item = InvoiceLineItem.builder()
                .productSku("SKU-1")
                .productName("Widget A")
                .unitPrice(new BigDecimal("10.00"))
                .quantity(2)
                .lineTotal(new BigDecimal("20.00"))
                .build();

        invoice.addLineItem(item);

        assertEquals(1, invoice.getLineItems().size());
        assertSame(invoice, item.getInvoice(), "Line item should reference parent invoice");
    }

    @Test
    @DisplayName("Product entity defaults and optimistic locking version field")
    void testProductEntity() {
        Product product = Product.builder()
                .sku("SKU-PROD-1")
                .name("Keyboard")
                .unitPrice(new BigDecimal("49.99"))
                .costPrice(new BigDecimal("25.00"))
                .quantityInStock(100)
                .reorderThreshold(15)
                .active(true)
                .build();

        assertTrue(product.getActive());
        assertEquals("SKU-PROD-1", product.getSku());
        assertNull(product.getVersion(), "Version starts null before JPA persistence");
    }

    @Test
    @DisplayName("User entity stores role and active state")
    void testUserEntity() {
        User user = User.builder()
                .username("clerk1")
                .email("clerk1@example.com")
                .passwordHash("hashed")
                .role(Role.BILLING_CLERK)
                .active(true)
                .build();

        assertEquals(Role.BILLING_CLERK, user.getRole());
        assertTrue(user.getActive());
    }
}
