package com.barinventory.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.admin.entity.Bar;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarRepository extends JpaRepository<Bar, Long> {
    
    List<Bar> findByActiveTrue();
    
    Optional<Bar> findByBarName(String barName);
    
    boolean existsByBarName(String barName);
}
