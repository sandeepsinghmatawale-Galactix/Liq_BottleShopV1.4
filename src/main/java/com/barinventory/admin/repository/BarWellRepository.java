package com.barinventory.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.BarWell;


//com/barinventory/repository/BarWellRepository.java
@Repository
public interface BarWellRepository extends JpaRepository<BarWell, Long> {
 List<BarWell> findByBarBarIdAndActiveTrue(Long barId);
 void deleteByBarBarId(Long barId);
 
}

