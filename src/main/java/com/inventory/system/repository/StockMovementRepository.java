package com.inventory.system.repository;

import com.inventory.system.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
