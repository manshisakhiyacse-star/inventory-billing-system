package com.inventory.system.repository;

import com.inventory.system.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /** Pessimistic write lock for concurrent stock updates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    /** Products below their reorder threshold that are still active. */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.quantityInStock <= p.reorderThreshold")
    Page<Product> findLowStockProducts(Pageable pageable);

    /** Search by name or SKU, case-insensitive. */
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByActiveTrue();
}
