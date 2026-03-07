package com.barinventory.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Find a user by email (used by Spring Security)
    Optional<User> findByEmail(String email);

    // ✅ Check if email already exists
    boolean existsByEmail(String email);

    // ✅ Count users by bar and role
    long countByBar_BarIdAndRole(Long barId, Role role);

    // ✅ Find users by role
    List<User> findByRole(Role role);

    // ✅ Find all admins (users with null bar)
    List<User> findByRoleAndBarIsNull(Role role);

    // ✅ Find active users for a bar
    List<User> findByBar_BarIdAndActiveTrue(Long barId);

    // ✅ Find all users of a bar (active or inactive)
    List<User> findByBar_BarId(Long barId);

}