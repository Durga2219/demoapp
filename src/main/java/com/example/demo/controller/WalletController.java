package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.WalletTransaction;
import com.example.demo.service.WalletService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;

    /**
     * Get wallet balance and details
     * GET /api/wallet/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getWalletBalance(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            Optional<Wallet> walletOpt = walletService.getWalletByUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wallet details retrieved successfully");
            
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                Map<String, Object> walletData = new HashMap<>();
                walletData.put("balance", wallet.getBalance());
                walletData.put("totalEarnings", wallet.getTotalEarnings());
                walletData.put("totalWithdrawn", wallet.getTotalWithdrawn());
                walletData.put("pendingAmount", wallet.getPendingAmount());
                walletData.put("availableBalance", wallet.getAvailableBalance());
                walletData.put("createdAt", wallet.getCreatedAt());
                walletData.put("updatedAt", wallet.getUpdatedAt());
                
                response.put("data", walletData);
            } else {
                // Create wallet if doesn't exist
                Wallet newWallet = walletService.createWallet(user);
                Map<String, Object> walletData = new HashMap<>();
                walletData.put("balance", newWallet.getBalance());
                walletData.put("totalEarnings", newWallet.getTotalEarnings());
                walletData.put("totalWithdrawn", newWallet.getTotalWithdrawn());
                walletData.put("pendingAmount", newWallet.getPendingAmount());
                walletData.put("availableBalance", newWallet.getAvailableBalance());
                walletData.put("createdAt", newWallet.getCreatedAt());
                walletData.put("updatedAt", newWallet.getUpdatedAt());
                
                response.put("data", walletData);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting wallet balance: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get wallet balance: " + e.getMessage()));
        }
    }

    /**
     * Get wallet statistics
     * GET /api/wallet/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWalletStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            WalletService.WalletStats stats = walletService.getWalletStats(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wallet statistics retrieved successfully");
            
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("currentBalance", stats.getCurrentBalance());
            statsData.put("totalEarnings", stats.getTotalEarnings());
            statsData.put("totalWithdrawn", stats.getTotalWithdrawn());
            statsData.put("pendingAmount", stats.getPendingAmount());
            statsData.put("totalTransactions", stats.getTotalTransactions());
            
            response.put("data", statsData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting wallet stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get wallet stats: " + e.getMessage()));
        }
    }

    /**
     * Get transaction history
     * GET /api/wallet/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            List<WalletTransaction> transactions;
            if (limit > 0 && limit <= 100) {
                transactions = walletService.getRecentTransactions(user, limit);
            } else {
                transactions = walletService.getTransactionHistory(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transaction history retrieved successfully");
            response.put("data", transactions.stream().map(this::mapTransactionToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting transaction history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get transaction history: " + e.getMessage()));
        }
    }

    /**
     * Request withdrawal
     * POST /api/wallet/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> requestWithdrawal(@RequestBody Map<String, Object> request,
                                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            Double amount = Double.valueOf(request.get("amount").toString());
            String description = request.getOrDefault("description", "Withdrawal request").toString();

            if (amount <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Amount must be greater than 0"));
            }

            WalletTransaction transaction = walletService.withdrawMoney(user, amount, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Withdrawal processed successfully");
            response.put("data", mapTransactionToResponse(transaction));

            logger.info("Withdrawal processed for user {}: ₹{}", username, amount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing withdrawal: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Withdrawal failed: " + e.getMessage()));
        }
    }

    /**
     * Get earnings summary
     * GET /api/wallet/earnings
     */
    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> getEarningsSummary(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            List<WalletTransaction> earnings = walletService.getTransactionHistoryByType(user, 
                com.example.demo.entity.TransactionType.CREDIT);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Earnings summary retrieved successfully");
            response.put("data", earnings.stream().map(this::mapTransactionToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting earnings summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get earnings summary: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> mapTransactionToResponse(WalletTransaction transaction) {
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("id", transaction.getId());
        transactionData.put("type", transaction.getTransactionType().toString());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("description", transaction.getDescription());
        transactionData.put("referenceId", transaction.getReferenceId());
        transactionData.put("balanceBefore", transaction.getBalanceBefore());
        transactionData.put("balanceAfter", transaction.getBalanceAfter());
        transactionData.put("status", transaction.getStatus().toString());
        transactionData.put("createdAt", transaction.getCreatedAt());
        return transactionData;
    }
}