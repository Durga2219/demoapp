package com.example.demo.repository;

import com.example.demo.entity.Wallet;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Find wallet by user
     */
    Optional<Wallet> findByUser(User user);

    /**
     * Find wallet by user ID
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Check if user has wallet
     */
    boolean existsByUser(User user);

    /**
     * Find all wallets with balance greater than specified amount
     */
    @Query("SELECT w FROM Wallet w WHERE w.balance > :minBalance ORDER BY w.balance DESC")
    List<Wallet> findWalletsWithBalanceGreaterThan(@Param("minBalance") Double minBalance);

    /**
     * Get total platform earnings (sum of all earnings)
     */
    @Query("SELECT COALESCE(SUM(w.totalEarnings), 0) FROM Wallet w")
    Double getTotalPlatformEarnings();

    /**
     * Get total platform withdrawals
     */
    @Query("SELECT COALESCE(SUM(w.totalWithdrawn), 0) FROM Wallet w")
    Double getTotalPlatformWithdrawals();

    /**
     * Get wallets with pending withdrawals
     */
    @Query("SELECT w FROM Wallet w WHERE w.pendingAmount > 0")
    List<Wallet> findWalletsWithPendingAmount();

    /**
     * Find top earning drivers
     */
    @Query("SELECT w FROM Wallet w ORDER BY w.totalEarnings DESC")
    List<Wallet> findTopEarningDrivers();

    /**
     * Get wallet statistics
     */
    @Query("SELECT COUNT(w), COALESCE(SUM(w.balance), 0), COALESCE(SUM(w.totalEarnings), 0), COALESCE(SUM(w.totalWithdrawn), 0) FROM Wallet w")
    Object[] getWalletStatistics();
}