package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ==================== BASIC FINDS ====================

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    // ==================== EXISTENCE CHECKS ====================

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // ==================== ROLE QUERIES ====================

    List<User> findByRole(Role role);
    long countByRole(Role role);

    // ==================== ACCOUNT STATUS ====================

    List<User> findByEnabledTrue();
    List<User> findByAccountNonLockedFalse();

    // ==================== DRIVER QUERIES ====================

    @Query("SELECT u FROM User u WHERE (u.role = com.example.demo.enums.Role.DRIVER OR u.role = com.example.demo.enums.Role.BOTH) " +
           "AND u.vehicleModel IS NOT NULL AND u.enabled = true")
    List<User> findAvailableDrivers();

    @Query("SELECT u FROM User u WHERE (u.role = com.example.demo.enums.Role.DRIVER OR u.role = com.example.demo.enums.Role.BOTH) " +
           "AND u.rating >= :minRating ORDER BY u.rating DESC")
    List<User> findTopRatedDrivers(@Param("minRating") Double minRating);

    // ==================== LOGIN (USERNAME OR EMAIL) ====================

    // ✅ Correct version — matches AuthService call (2 params)
    Optional<User> findByUsernameOrEmail(String username, String email);

    // ==================== SEARCH & ANALYTICS ====================

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByName(@Param("name") String name);

    @Query(value = "SELECT * FROM app_users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY created_at DESC", nativeQuery = true)
    List<User> findRecentUsers();
}
