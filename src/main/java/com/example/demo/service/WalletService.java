package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.WalletRepository;
import com.example.demo.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    /**
     * Create wallet for new user (drivers)
     */
    public Wallet createWallet(User user) {
        try {
            logger.info("Creating wallet for user: {}", user.getUsername());

            // Check if wallet already exists
            Optional<Wallet> existingWallet = walletRepository.findByUser(user);
            if (existingWallet.isPresent()) {
                logger.info("Wallet already exists for user: {}", user.getUsername());
                return existingWallet.get();
            }

            Wallet wallet = new Wallet(user);
            Wallet savedWallet = walletRepository.save(wallet);

            logger.info("Wallet created successfully for user: {} with ID: {}", user.getUsername(), savedWallet.getId());
            return savedWallet;

        } catch (Exception e) {
            logger.error("Error creating wallet for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Failed to create wallet: " + e.getMessage());
        }
    }

    /**
     * Get or create wallet for user
     */
    public Wallet getOrCreateWallet(User user) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        return walletOpt.orElseGet(() -> createWallet(user));
    }

    /**
     * Get wallet by user
     */
    public Optional<Wallet> getWalletByUser(User user) {
        return walletRepository.findByUser(user);
    }

    /**
     * Get wallet balance
     */
    public Double getBalance(User user) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        return walletOpt.map(Wallet::getBalance).orElse(0.0);
    }

    /**
     * Add money to wallet (driver earnings)
     */
    public WalletTransaction addMoney(User user, Double amount, String description, String referenceId) {
        try {
            logger.info("Adding ₹{} to wallet for user: {}", amount, user.getUsername());

            if (amount <= 0) {
                throw new RuntimeException("Amount must be greater than 0");
            }

            Wallet wallet = getOrCreateWallet(user);
            Double balanceBefore = wallet.getBalance();

            // Add earnings to wallet
            wallet.addEarnings(amount);
            walletRepository.save(wallet);

            // Create transaction record
            WalletTransaction transaction = new WalletTransaction(
                wallet, TransactionType.CREDIT, amount, description, referenceId,
                balanceBefore, wallet.getBalance()
            );

            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

            logger.info("Successfully added ₹{} to wallet. New balance: ₹{}", amount, wallet.getBalance());
            return savedTransaction;

        } catch (Exception e) {
            logger.error("Error adding money to wallet for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Failed to add money to wallet: " + e.getMessage());
        }
    }

    /**
     * Withdraw money from wallet
     */
    public WalletTransaction withdrawMoney(User user, Double amount, String description) {
        try {
            logger.info("Withdrawing ₹{} from wallet for user: {}", amount, user.getUsername());

            if (amount <= 0) {
                throw new RuntimeException("Amount must be greater than 0");
            }

            Optional<Wallet> walletOpt = walletRepository.findByUser(user);
            if (walletOpt.isEmpty()) {
                throw new RuntimeException("Wallet not found for user");
            }

            Wallet wallet = walletOpt.get();

            if (wallet.getAvailableBalance() < amount) {
                throw new RuntimeException("Insufficient balance. Available: ₹" + wallet.getAvailableBalance());
            }

            Double balanceBefore = wallet.getBalance();

            // Deduct amount from wallet
            wallet.deductAmount(amount);
            walletRepository.save(wallet);

            // Create transaction record
            WalletTransaction transaction = new WalletTransaction(
                wallet, TransactionType.WITHDRAWAL, amount, description, null,
                balanceBefore, wallet.getBalance()
            );

            WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

            logger.info("Successfully withdrew ₹{} from wallet. New balance: ₹{}", amount, wallet.getBalance());
            return savedTransaction;

        } catch (Exception e) {
            logger.error("Error withdrawing money from wallet for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Failed to withdraw money: " + e.getMessage());
        }
    }

    /**
     * Get transaction history for user
     */
    public List<WalletTransaction> getTransactionHistory(User user) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        if (walletOpt.isEmpty()) {
            return List.of(); // Return empty list if no wallet
        }

        return walletTransactionRepository.findByWalletOrderByCreatedAtDesc(walletOpt.get());
    }

    /**
     * Get transaction history by type
     */
    public List<WalletTransaction> getTransactionHistoryByType(User user, TransactionType transactionType) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        if (walletOpt.isEmpty()) {
            return List.of();
        }

        return walletTransactionRepository.findByWalletAndTransactionTypeOrderByCreatedAtDesc(
            walletOpt.get(), transactionType
        );
    }

    /**
     * Get recent transactions (last 10)
     */
    public List<WalletTransaction> getRecentTransactions(User user, int limit) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        if (walletOpt.isEmpty()) {
            return List.of();
        }

        return walletTransactionRepository.findRecentTransactions(walletOpt.get(), limit);
    }

    /**
     * Check if transaction already exists for reference ID
     */
    public boolean transactionExists(String referenceId) {
        return walletTransactionRepository.existsByReferenceId(referenceId);
    }

    /**
     * Get wallet statistics for user
     */
    public WalletStats getWalletStats(User user) {
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        if (walletOpt.isEmpty()) {
            return new WalletStats(0.0, 0.0, 0.0, 0.0, 0);
        }

        Wallet wallet = walletOpt.get();
        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);

        return new WalletStats(
            wallet.getBalance(),
            wallet.getTotalEarnings(),
            wallet.getTotalWithdrawn(),
            wallet.getPendingAmount(),
            transactions.size()
        );
    }

    /**
     * Process driver payment from ride completion
     */
    public WalletTransaction processDriverPayment(User driver, Double totalFare, String rideDescription, String paymentId) {
        try {
            // Check if payment already processed
            if (transactionExists(paymentId)) {
                logger.warn("Payment {} already processed for driver {}", paymentId, driver.getUsername());
                return walletTransactionRepository.findByReferenceId(paymentId).orElse(null);
            }

            // Calculate driver earnings (90% of total fare)
            Double platformCommission = totalFare * 0.10; // 10% commission
            Double driverEarnings = totalFare - platformCommission;

            String description = "Ride earnings: " + rideDescription + " (₹" + totalFare + " - ₹" + platformCommission + " commission)";

            return addMoney(driver, driverEarnings, description, paymentId);

        } catch (Exception e) {
            logger.error("Error processing driver payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process driver payment: " + e.getMessage());
        }
    }

    /**
     * Get total platform earnings
     */
    public Double getTotalPlatformEarnings() {
        return walletRepository.getTotalPlatformEarnings();
    }

    /**
     * Get total platform withdrawals
     */
    public Double getTotalPlatformWithdrawals() {
        return walletRepository.getTotalPlatformWithdrawals();
    }

    // Inner class for wallet statistics
    public static class WalletStats {
        private Double currentBalance;
        private Double totalEarnings;
        private Double totalWithdrawn;
        private Double pendingAmount;
        private Integer totalTransactions;

        public WalletStats(Double currentBalance, Double totalEarnings, Double totalWithdrawn, 
                          Double pendingAmount, Integer totalTransactions) {
            this.currentBalance = currentBalance;
            this.totalEarnings = totalEarnings;
            this.totalWithdrawn = totalWithdrawn;
            this.pendingAmount = pendingAmount;
            this.totalTransactions = totalTransactions;
        }

        // Getters
        public Double getCurrentBalance() { return currentBalance; }
        public Double getTotalEarnings() { return totalEarnings; }
        public Double getTotalWithdrawn() { return totalWithdrawn; }
        public Double getPendingAmount() { return pendingAmount; }
        public Integer getTotalTransactions() { return totalTransactions; }
    }
}