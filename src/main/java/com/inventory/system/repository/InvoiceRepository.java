package com.inventory.system.repository;

import com.inventory.system.entity.Invoice;
import com.inventory.system.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByCustomerId(Long customerId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :start AND i.createdAt <= :end")
    Page<Invoice> findByDateRange(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  Pageable pageable);

    /** Count invoices created today for generating sequential invoice numbers. */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.createdAt >= :startOfDay")
    long countInvoicesToday(@Param("startOfDay") LocalDateTime startOfDay);

    /** Sales total for a date range (non-cancelled invoices). */
    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i " +
           "WHERE i.status NOT IN ('CANCELLED', 'VOIDED') " +
           "AND i.createdAt >= :start AND i.createdAt <= :end")
    BigDecimal sumGrandTotalByDateRange(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    /** Count of invoices in a date range by status. */
    long countByStatusAndCreatedAtBetween(InvoiceStatus status,
                                          LocalDateTime start, LocalDateTime end);
}
