package com.barinventory.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.DistributionRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Long> {
    
    List<DistributionRecord> findBySessionSessionId(Long sessionId);
    
    Optional<DistributionRecord> findBySessionSessionIdAndProductProductId(
        Long sessionId, Long productId);
}
