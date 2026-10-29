package com.inventory.system.service;

import com.inventory.system.dto.request.InvoiceLineItemRequest;
import com.inventory.system.dto.request.InvoiceRequest;
import com.inventory.system.dto.response.InvoiceResponse;
import com.inventory.system.entity.Customer;
import com.inventory.system.entity.Invoice;
import com.inventory.system.entity.InvoiceLineItem;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.entity.enums.PaymentMethod;
import com.inventory.system.entity.enums.PaymentStatus;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.InvalidOperationException;
import com.inventory.system.mapper.InvoiceMapper;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.InvoiceRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Should atomically create invoice, decrement stock, and calculate financials correctly")
    void testCreateInvoiceSuccess() {
        Customer customer = Customer.builder()
                .id(1L)
                .name("Acme Corp")
                .active(true)
                .build();

        Product product = Product.builder()
                .id(10L)
                .sku("SKU-MOUSE")
                .name("Wireless Mouse")
                .unitPrice(new BigDecimal("20.00"))
                .quantityInStock(50)
                .active(true)
                .build();

        InvoiceRequest request = InvoiceRequest.builder()
                .customerId(1L)
                .lineItems(List.of(
                        new InvoiceLineItemRequest(10L, 2) // 2 * $20.00 = $40.00
                ))
                .discountAmount(new BigDecimal("5.00")) // $40 - $5 = $35
                .taxRate(new BigDecimal("0.1000")) // 10% tax on $35 = $3.50
                .paymentMethod(PaymentMethod.CARD)
                .build();

        Invoice savedInvoice = Invoice.builder()
                .id(100L)
                .invoiceNumber("INV-20261029-0001")
                .subtotal(new BigDecimal("40.00"))
                .discountAmount(new BigDecimal("5.00"))
                .taxAmount(new BigDecimal("3.50"))
                .grandTotal(new BigDecimal("38.50"))
                .status(InvoiceStatus.CREATED)
                .build();

        InvoiceResponse responseDto = InvoiceResponse.builder()
                .id(100L)
                .invoiceNumber("INV-20261029-0001")
                .subtotal(new BigDecimal("40.00"))
                .grandTotal(new BigDecimal("38.50"))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.countInvoicesToday(any())).thenReturn(0L);
        when(invoiceRepository.findByInvoiceNumber(anyString())).thenReturn(Optional.empty());
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(invoiceMapper.toResponse(savedInvoice)).thenReturn(responseDto);

        InvoiceResponse result = invoiceService.createInvoice(request);

        assertNotNull(result);
        assertEquals(48, product.getQuantityInStock(), "Stock must be decremented by 2");
        verify(stockMovementRepository, times(1)).save(any());
        verify(auditService, times(1)).logAction(eq("CREATE_INVOICE"), eq("Invoice"), eq(100L), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested item quantity exceeds stock")
    void testCreateInvoiceInsufficientStock() {
        Customer customer = Customer.builder().id(1L).name("Acme").active(true).build();
        Product product = Product.builder()
                .id(10L)
                .sku("SKU-LOW")
                .unitPrice(new BigDecimal("10.00"))
                .quantityInStock(1)
                .active(true)
                .build();

        InvoiceRequest request = InvoiceRequest.builder()
                .customerId(1L)
                .lineItems(List.of(new InvoiceLineItemRequest(10L, 5)))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(invoiceRepository.countInvoicesToday(any())).thenReturn(0L);
        when(invoiceRepository.findByInvoiceNumber(anyString())).thenReturn(Optional.empty());
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> invoiceService.createInvoice(request));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel invoice and idempotently restore inventory")
    void testCancelInvoiceRestoresStockIdempotently() {
        Product product = Product.builder()
                .id(10L)
                .sku("SKU-A")
                .quantityInStock(10)
                .build();

        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-20261029-0001")
                .status(InvoiceStatus.CREATED)
                .stockRestored(false)
                .build();

        InvoiceLineItem item = InvoiceLineItem.builder()
                .product(product)
                .quantity(3)
                .build();
        invoice.addLineItem(item);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(new InvoiceResponse());

        invoiceService.cancelInvoice(1L, "Wrong item selected");

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        assertTrue(invoice.getStockRestored());
        assertEquals(13, product.getQuantityInStock(), "Stock should be restored from 10 to 13");
        verify(stockMovementRepository, times(1)).save(any());

        // Call cancel again - should throw InvalidOperationException (already cancelled)
        assertThrows(InvalidOperationException.class, () -> invoiceService.cancelInvoice(1L, "Second cancel"));
    }

    @Test
    @DisplayName("Should update status to PAID on markInvoicePaid")
    void testMarkInvoicePaid() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .status(InvoiceStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(new InvoiceResponse());

        invoiceService.markInvoicePaid(1L);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(PaymentStatus.COMPLETED, invoice.getPaymentStatus());
    }
}
