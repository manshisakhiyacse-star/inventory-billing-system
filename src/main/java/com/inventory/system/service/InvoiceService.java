package com.inventory.system.service;

import com.inventory.system.dto.request.InvoiceLineItemRequest;
import com.inventory.system.dto.request.InvoiceRequest;
import com.inventory.system.dto.response.InvoiceResponse;
import com.inventory.system.entity.Customer;
import com.inventory.system.entity.Invoice;
import com.inventory.system.entity.InvoiceLineItem;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.StockMovement;
import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.entity.enums.PaymentStatus;
import com.inventory.system.entity.enums.StockMovementType;
import com.inventory.system.exception.InsufficientStockException;
import com.inventory.system.exception.InvalidOperationException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.InvoiceMapper;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.InvoiceRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InvoiceMapper invoiceMapper;
    private final AuditService auditService;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new InvalidOperationException("Cannot generate invoice for inactive customer: " + customer.getName());
        }

        String username = getCurrentUsername();
        String invoiceNumber = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .status(InvoiceStatus.CREATED)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(request.getPaymentMethod() != null ? PaymentStatus.COMPLETED : PaymentStatus.PENDING)
                .notes(request.getNotes())
                .createdBy(username)
                .stockRestored(false)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<StockMovement> stockMovements = new ArrayList<>();

        for (InvoiceLineItemRequest itemReq : request.getLineItems()) {
            // Acquire pessimistic write lock (SELECT ... FOR UPDATE) on the product row
            Product product = productRepository.findByIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new InvalidOperationException("Cannot bill inactive product: " + product.getSku());
            }

            int availableStock = product.getQuantityInStock();
            int requestedQuantity = itemReq.getQuantity();

            if (availableStock < requestedQuantity) {
                throw new InsufficientStockException(
                        product.getId(),
                        product.getSku(),
                        requestedQuantity,
                        availableStock
                );
            }

            // Decrement inventory
            int newStock = availableStock - requestedQuantity;
            product.setQuantityInStock(newStock);
            productRepository.save(product);

            // Calculate item total using BigDecimal with HALF_UP rounding
            BigDecimal lineTotal = product.getUnitPrice()
                    .multiply(BigDecimal.valueOf(requestedQuantity))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            // Build line item capturing immutable snapshot of SKU, name, price at sale time
            InvoiceLineItem lineItem = InvoiceLineItem.builder()
                    .product(product)
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .unitPrice(product.getUnitPrice())
                    .quantity(requestedQuantity)
                    .lineTotal(lineTotal)
                    .build();

            invoice.addLineItem(lineItem);

            // Prepare stock movement record
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .movementType(StockMovementType.SALE)
                    .quantityChange(-requestedQuantity)
                    .quantityAfter(newStock)
                    .reason("Sale on invoice " + invoiceNumber)
                    .performedBy(username)
                    .build();

            stockMovements.add(movement);
        }

        // Apply discount
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal; // Discount cannot exceed subtotal
        }
        BigDecimal discountedSubtotal = subtotal.subtract(discount);

        // Apply single tax rate
        BigDecimal taxRate = request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = discountedSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = discountedSubtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discount);
        invoice.setTaxRate(taxRate);
        invoice.setTaxAmount(taxAmount);
        invoice.setGrandTotal(grandTotal);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Persist stock movement audit records linked to the created invoice
        for (StockMovement movement : stockMovements) {
            movement.setReferenceId(savedInvoice.getId());
            stockMovementRepository.save(movement);
        }

        auditService.logAction(
                "CREATE_INVOICE",
                "Invoice",
                savedInvoice.getId(),
                String.format("Generated invoice %s for customer %s. Grand Total: %s",
                        savedInvoice.getInvoiceNumber(), customer.getName(), savedInvoice.getGrandTotal()),
                username
        );

        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new InvalidOperationException("Invoice is already " + invoice.getStatus());
        }

        String username = getCurrentUsername();
        invoice.setStatus(InvoiceStatus.CANCELLED);

        // Idempotent inventory restoration check
        if (!Boolean.TRUE.equals(invoice.getStockRestored())) {
            for (InvoiceLineItem lineItem : invoice.getLineItems()) {
                Product product = productRepository.findByIdWithLock(lineItem.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineItem.getProduct().getId()));

                int restoredStock = product.getQuantityInStock() + lineItem.getQuantity();
                product.setQuantityInStock(restoredStock);
                productRepository.save(product);

                StockMovement reversal = StockMovement.builder()
                        .product(product)
                        .movementType(StockMovementType.CANCELLATION_REVERSAL)
                        .quantityChange(lineItem.getQuantity())
                        .quantityAfter(restoredStock)
                        .referenceId(invoice.getId())
                        .reason("Stock restored on invoice cancellation: " + invoice.getInvoiceNumber())
                        .performedBy(username)
                        .build();

                stockMovementRepository.save(reversal);
            }
            invoice.setStockRestored(true);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        auditService.logAction(
                "CANCEL_INVOICE",
                "Invoice",
                savedInvoice.getId(),
                "Cancelled invoice " + savedInvoice.getInvoiceNumber() + ". Reason: " + reason,
                username
        );

        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional
    public InvoiceResponse markInvoicePaid(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new InvalidOperationException("Cannot mark a " + invoice.getStatus() + " invoice as paid.");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentStatus(PaymentStatus.COMPLETED);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        auditService.logAction("PAY_INVOICE", "Invoice", savedInvoice.getId(),
                "Marked invoice " + savedInvoice.getInvoiceNumber() + " as PAID.");

        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceNumber", invoiceNumber));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(InvoiceStatus status, Long customerId, Pageable pageable) {
        Page<Invoice> page;
        if (status != null) {
            page = invoiceRepository.findByStatus(status, pageable);
        } else if (customerId != null) {
            page = invoiceRepository.findByCustomerId(customerId, pageable);
        } else {
            page = invoiceRepository.findAll(pageable);
        }
        return page.map(invoiceMapper::toResponse);
    }

    private synchronized String generateInvoiceNumber() {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime startOfDay = today.atStartOfDay();

        long sequence = invoiceRepository.countInvoicesToday(startOfDay) + 1;
        String invoiceNumber = String.format("INV-%s-%04d", datePrefix, sequence);

        // Collision safety check
        while (invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent()) {
            sequence++;
            invoiceNumber = String.format("INV-%s-%04d", datePrefix, sequence);
        }

        return invoiceNumber;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
