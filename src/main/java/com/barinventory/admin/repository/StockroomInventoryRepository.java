package com.barinventory.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.StockroomInventory;

import jakarta.transaction.Transactional;

@Repository
public interface StockroomInventoryRepository extends JpaRepository<StockroomInventory, Long> {
    
    List<StockroomInventory> findBySessionSessionId(Long sessionId);
    
    Optional<StockroomInventory> findBySessionSessionIdAndProductProductId(
        Long sessionId, Long productId);
    
    
    

    // ✅ ADD THIS
    @Modifying
    @Transactional
    void deleteBySessionSessionId(Long sessionId);
    
    
 // Add this to StockroomInventoryRepository.java
    @Query("SELECT s FROM StockroomInventory s " +
           "WHERE s.session.sessionId = :sessionId")
    List<StockroomInventory> findBySessionSessionIdWithProduct(
            @Param("sessionId") Long sessionId);
    
}
