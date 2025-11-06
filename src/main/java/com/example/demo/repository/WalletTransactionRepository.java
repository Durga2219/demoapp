package com.example.demo.repository;

import com.example.demo.entity.WalletTransaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.TransactionType;
import com.example.demo.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find all transactions for a wallet
     */
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    /**
     * Find transactions by wallet and type
     */
    List<WalletTransaction> findByWalletAndTransactionTypeOrderByCreatedAtDesc(Wallet wallet, TransactionType transactionType);

    /**
     * Find transactions by wallet and status
     */
    List<WalletTransaction> findByWalletAndStatusOrderByCreatedAtDesc(Wallet wallet, TransactionStatus status);

    /**
     * Find transaction by reference ID
     */
    Optional<WalletTransaction> findByReferenceId(String referenceId);

    /**
     * Find transactions within date range
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet = :wallet " +
           "AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findByWalletAndDateRange(
            @Param("wallet") Wallet wallet,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get total amount by transaction type for a wallet
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt " +
           "WHERE wt.wallet = :wallet AND wt.transactionType = :transactionType AND wt.status = 'COMPLETED'")
    Double getTotalAmountByType(@Param("wallet") Wallet wallet, @Param("transactionType") TransactionType transactionType);

    /**
     * Find pending transactions
     */
    List<WalletTransaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status);

    /**
     * Get transaction statistics for a wallet
     */
    @Query("SELECT wt.transactionType, COUNT(wt), COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt " +
           "WHERE wt.wallet = :wallet AND wt.status = 'COMPLETED' GROUP BY wt.transactionType")
    List<Object[]> getTransactionStatistics(@Param("wallet") Wallet wallet);

    /**
     * Find recent transactions (last N transactions)
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.wallet = :wallet " +
           "ORDER BY wt.createdAt DESC LIMIT :limit")
    List<WalletTransaction> findRecentTransactions(@Param("wallet") Wallet wallet, @Param("limit") int limit);

    /**
     * Check if transaction exists for reference ID
     */
    boolean existsByReferenceId(String referenceId);
}