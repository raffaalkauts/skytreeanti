package com.wiredid.skytree.banking.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.*;
import com.wiredid.skytree.banking.persistence.BankPersistenceService;
import com.wiredid.skytree.banking.util.BankUtil;
import com.wiredid.skytree.banking.util.RateLimiter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of BankService with proper double-entry bookkeeping
 */
public class BankServiceImpl implements BankService {

    private final SkytreePlugin plugin;
    private final EconomyService economyService;
    private final BankPersistenceService persistenceService;
    private final RateLimiter rateLimiter;

    // Configuration
    private double globalInterestRate = 0.0011; // 0.11% per minute = 6.6% per hour

    public BankServiceImpl(SkytreePlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.persistenceService = new BankPersistenceService(plugin);
        this.rateLimiter = new RateLimiter(plugin.getConfig().getInt("bank.rate_limit_per_minute", 10));

        this.globalInterestRate = plugin.getConfig().getDouble("bank.interest_rate_per_minute", 0.0011);

        // Start interest calculation task
        startInterestTask();
    }

    @Override
    public BankAccount getAccount(UUID playerId) {
        BankAccount account = persistenceService.loadAccount(playerId);
        if (account == null) {
            createAccount(playerId);
            account = persistenceService.loadAccount(playerId);
        }
        return account;
    }

    @Override
    public boolean createAccount(UUID playerId) {
        if (hasAccount(playerId)) {
            return false;
        }

        BankAccount account = new BankAccount(playerId);
        account.setInterestRate(this.globalInterestRate); // Use current global rate
        persistenceService.saveAccount(account);
        return true;
    }

    @Override
    public boolean hasAccount(UUID playerId) {
        return persistenceService.loadAccount(playerId) != null;
    }

    private long getMaxDeposit() {
        return BankUtil.toCents(plugin.getConfig().getDouble("bank.max_deposit", 1_000_000_000_000L));
    }

    private long getMaxWithdraw() {
        return BankUtil.toCents(plugin.getConfig().getDouble("bank.max_withdraw", 1_000_000_000_000L));
    }

    private long getMaxTransfer() {
        return BankUtil.toCents(plugin.getConfig().getDouble("bank.max_transfer", 1_000_000_000_000L));
    }

    private long getMinBalanceForInterest() {
        return BankUtil.toCents(plugin.getConfig().getDouble("bank.min_balance_for_interest", 10.0));
    }

    public TransactionResult deposit(UUID playerId, long amount) {
        // Validation
        if (amount <= 0) {
            return TransactionResult.failure("§cAmount must be positive!");
        }

        if (amount > getMaxDeposit()) {
            return TransactionResult.failure("§cMaximum deposit is " + BankUtil.formatCurrency(getMaxDeposit()) + "!");
        }

        // Rate limiting
        if (rateLimiter.isRateLimited(playerId)) {
            return TransactionResult.failure("§cToo many transactions! Please wait.");
        }

        // Check player has cash
        double cashUSDT = economyService.getBalance(playerId);
        long cashCents = BankUtil.toCents(cashUSDT);

        if (cashCents < amount) {
            return TransactionResult
                    .failure("§cInsufficient cash! You have " + BankUtil.formatCurrency(cashCents) + ".");
        }

        // Get account
        BankAccount account = getAccount(playerId);

        // Remove cash from player
        economyService.removeBalance(playerId, BankUtil.toUSDT(amount));


        // Add to bank account
        account.addBalance(amount);
        account.setTotalDeposited(account.getTotalDeposited() + amount);

        // Create transaction record
        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                txId,
                System.currentTimeMillis(),
                "CASH",
                playerId.toString(),
                amount,
                0, // No fee for deposits
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.COMPLETED,
                "Deposit to bank account");

        // Record journal entries (double-entry)
        recordDepositJournal(txId, amount);

        // Save
        persistenceService.saveAccount(account);
        persistenceService.saveTransaction(transaction, playerId);
        rateLimiter.recordTransaction(playerId);

        return TransactionResult.success(
                "§a§l[Bank] §7Deposited §e" + BankUtil.formatCurrency(amount) + " §7to your account.",
                transaction);
    }

    @Override
    public TransactionResult withdraw(UUID playerId, long amount) {
        // Validation
        if (amount <= 0) {
            return TransactionResult.failure("§cAmount must be positive!");
        }

        if (amount > getMaxWithdraw()) {
            return TransactionResult
                    .failure("§cMaximum withdrawal is " + BankUtil.formatCurrency(getMaxWithdraw()) + "!");
        }

        // Rate limiting
        if (rateLimiter.isRateLimited(playerId)) {
            return TransactionResult.failure("§cToo many transactions! Please wait.");
        }

        // Get account
        BankAccount account = getAccount(playerId);

        // Calculate fee
        long fee = calculateFee(amount);
        long totalDeduction = amount + fee;

        // Check balance
        if (!account.hasBalance(totalDeduction)) {
            return TransactionResult.failure(
                    "§cInsufficient balance! You need " + BankUtil.formatCurrency(totalDeduction)
                            + " §7(including fee).");
        }

        // Deduct from bank
        account.subtractBalance(totalDeduction);
        account.setTotalWithdrawn(account.getTotalWithdrawn() + amount);

        // Add cash to player
        economyService.addBalance(playerId, BankUtil.toUSDT(amount));

        // Create transaction record
        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                txId,
                System.currentTimeMillis(),
                playerId.toString(),
                "CASH",
                amount,
                fee,
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.COMPLETED,
                "Withdrawal from bank account");

        // Record journal entries
        recordWithdrawJournal(txId, amount, fee);

        // Save
        persistenceService.saveAccount(account);
        persistenceService.saveTransaction(transaction, playerId);
        rateLimiter.recordTransaction(playerId);

        return TransactionResult.success(
                "§a§l[Bank] §7Withdrew §e" + BankUtil.formatCurrency(amount) + " §7(Fee: §c"
                        + BankUtil.formatCurrency(fee)
                        + "§7).",
                transaction);
    }

    @Override
    public TransactionResult transfer(UUID fromPlayer, UUID toPlayer, long amount) {
        // Validation
        if (amount <= 0) {
            return TransactionResult.failure("§cAmount must be positive!");
        }

        if (amount > getMaxTransfer()) {
            return TransactionResult
                    .failure("§cMaximum transfer is " + BankUtil.formatCurrency(getMaxTransfer()) + "!");
        }

        if (fromPlayer.equals(toPlayer)) {
            return TransactionResult.failure("§cCannot transfer to yourself!");
        }

        // Rate limiting
        if (rateLimiter.isRateLimited(fromPlayer)) {
            return TransactionResult.failure("§cToo many transactions! Please wait.");
        }

        // Get accounts
        BankAccount fromAccount = getAccount(fromPlayer);
        BankAccount toAccount = getAccount(toPlayer);

        // Calculate fee
        long fee = calculateFee(amount);
        long totalDeduction = amount + fee;

        // Check balance
        if (!fromAccount.hasBalance(totalDeduction)) {
            return TransactionResult.failure(
                    "§cInsufficient balance! You need " + BankUtil.formatCurrency(totalDeduction)
                            + " §7(including fee).");
        }

        // Transfer
        fromAccount.subtractBalance(totalDeduction);
        toAccount.addBalance(amount);

        // Create transaction record
        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                txId,
                System.currentTimeMillis(),
                fromPlayer.toString(),
                toPlayer.toString(),
                amount,
                fee,
                Transaction.TransactionType.TRANSFER,
                Transaction.TransactionStatus.COMPLETED,
                "Transfer between accounts");

        // Record journal entries
        recordTransferJournal(txId, amount, fee);

        // Save
        persistenceService.saveAccount(fromAccount);
        persistenceService.saveAccount(toAccount);
        persistenceService.saveTransaction(transaction, fromPlayer);
        persistenceService.saveTransaction(transaction, toPlayer);
        rateLimiter.recordTransaction(fromPlayer);

        return TransactionResult.success(
                "§a§l[Bank] §7Transferred §e" + BankUtil.formatCurrency(amount) + " §7(Fee: §c"
                        + BankUtil.formatCurrency(fee)
                        + "§7).",
                transaction);
    }

    @Override
    public void calculateInterest(UUID playerId) {
        BankAccount account = getAccount(playerId);

        long now = System.currentTimeMillis();
        long elapsed = now - account.getLastInterestCalculation();
        long minutes = elapsed / 60000;

        double effectiveRate = plugin.getEconomyManager() != null
                ? plugin.getEconomyManager().getDynamicInterestRate()
                : account.getInterestRate();

        if (plugin.getEconomyManager() != null && effectiveRate != account.getInterestRate()) {
            account.setInterestRate(effectiveRate);
        }

        // Only calculate if at least 1 minute has passed and balance is sufficient
        if (minutes >= 1 && account.getBalance() >= getMinBalanceForInterest()) {
            long interest = BankUtil.calculateInterest(account.getBalance(), effectiveRate, minutes);

            if (interest > 0) {
                // Add interest
                account.addBalance(interest);
                account.setTotalInterestEarned(account.getTotalInterestEarned() + interest);
                account.setLastInterestCalculation(now);

                // Create transaction record
                UUID txId = UUID.randomUUID();
                Transaction transaction = new Transaction(
                        txId,
                        now,
                        "SYSTEM",
                        playerId.toString(),
                        interest,
                        0,
                        Transaction.TransactionType.INTEREST,
                        Transaction.TransactionStatus.COMPLETED,
                        "Interest payment");

                // Record journal entries
                recordInterestJournal(txId, interest);

                // Save
                persistenceService.saveAccount(account);
                persistenceService.saveTransaction(transaction, playerId);
            }
        }
    }

    @Override
    public void calculateAllInterest() {
        List<UUID> allPlayers = persistenceService.getAllAccountIds();
        for (UUID playerId : allPlayers) {
            calculateInterest(playerId);
        }
    }

    @Override
    public long getNextInterestAmount(UUID playerId) {
        BankAccount account = getAccount(playerId);
        if (account.getBalance() < getMinBalanceForInterest()) {
            return 0;
        }
        return BankUtil.calculateInterest(account.getBalance(), account.getInterestRate(), 1);
    }

    @Override
    public long getTimeUntilNextInterest(UUID playerId) {
        BankAccount account = getAccount(playerId);
        long now = System.currentTimeMillis();
        long elapsed = now - account.getLastInterestCalculation();
        long nextInterestIn = 60000 - (elapsed % 60000); // Time until next minute
        return nextInterestIn;
    }

    @Override
    public List<Transaction> getTransactionHistory(UUID playerId, int page, int pageSize) {
        List<Transaction> allTransactions = persistenceService.getTransactions(playerId);

        // Sort by timestamp descending (newest first)
        allTransactions.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // Paginate
        int start = page * pageSize;
        int end = Math.min(start + pageSize, allTransactions.size());

        if (start >= allTransactions.size()) {
            return new ArrayList<>();
        }

        return allTransactions.subList(start, end);
    }

    @Override
    public BankStats getStats(UUID playerId) {
        BankAccount account = getAccount(playerId);
        List<Transaction> transactions = persistenceService.getTransactions(playerId);

        long totalTransfers = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.TRANSFER)
                .count();

        long totalFeesPaid = transactions.stream()
                .mapToLong(Transaction::getFee)
                .sum();

        long accountAge = System.currentTimeMillis() - account.getCreatedAt();

        return new BankStats(
                playerId,
                account.getBalance(),
                account.getTotalDeposited(),
                account.getTotalWithdrawn(),
                account.getTotalInterestEarned(),
                totalTransfers,
                totalFeesPaid,
                accountAge);
    }

    @Override
    public boolean reconcile() {
        // Sum all player balances
        List<UUID> allPlayers = persistenceService.getAllAccountIds();
        long totalPlayerBalances = 0;

        for (UUID playerId : allPlayers) {
            BankAccount account = getAccount(playerId);
            totalPlayerBalances += account.getBalance();
        }

        // Sum all journal liabilities (PLAYER_DEPOSITS credits)
        long totalLiabilities = persistenceService.getTotalLiabilities();

        // They should match
        boolean balanced = totalPlayerBalances == totalLiabilities;

        if (!balanced) {
            plugin.getLogger().warning("[Bank] Reconciliation FAILED! Player balances: " + totalPlayerBalances
                    + ", Liabilities: " + totalLiabilities);
        } else {
            plugin.getLogger()
                    .info("[Bank] Reconciliation PASSED. Total: " + BankUtil.formatCurrency(totalPlayerBalances));
        }

        return balanced;
    }

    @Override
    public List<Transaction> getAuditTrail(UUID playerId, int days) {
        List<Transaction> allTransactions = persistenceService.getTransactions(playerId);
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

        return allTransactions.stream()
                .filter(t -> t.getTimestamp() >= cutoffTime)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public void setGlobalInterestRate(double rate) {
        this.globalInterestRate = rate;
        // In a real implementation, we should save this to config
    }

    @Override
    public double getGlobalInterestRate() {
        return this.globalInterestRate;
    }

    @Override
    public long calculateFee(long amount) {
        return BankUtil.calculateFee(amount);
    }

    @Override
    public String formatAmount(long amount) {
        return BankUtil.formatCurrency(amount);
    }

    // ========== DOUBLE-ENTRY BOOKKEEPING ==========

    private void recordDepositJournal(UUID txId, long amount) {
        List<JournalEntry> entries = new ArrayList<>();

        // DEBIT: CASH_VAULT (Asset increases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.ASSET,
                "CASH_VAULT",
                amount,
                0));

        // CREDIT: PLAYER_DEPOSITS (Liability increases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.LIABILITY,
                "PLAYER_DEPOSITS",
                0,
                amount));

        persistenceService.saveJournalEntries(entries);
    }

    private void recordWithdrawJournal(UUID txId, long amount, long fee) {
        List<JournalEntry> entries = new ArrayList<>();

        // DEBIT: PLAYER_DEPOSITS (Liability decreases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.LIABILITY,
                "PLAYER_DEPOSITS",
                amount + fee,
                0));

        // CREDIT: CASH_VAULT (Asset decreases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.ASSET,
                "CASH_VAULT",
                0,
                amount));

        // CREDIT: TRANSACTION_FEES (Revenue increases)
        if (fee > 0) {
            entries.add(new JournalEntry(
                    UUID.randomUUID(),
                    txId,
                    System.currentTimeMillis(),
                    JournalEntry.AccountType.REVENUE,
                    "TRANSACTION_FEES",
                    0,
                    fee));
        }

        persistenceService.saveJournalEntries(entries);
    }

    private void recordTransferJournal(UUID txId, long amount, long fee) {
        List<JournalEntry> entries = new ArrayList<>();

        // DEBIT: PLAYER_DEPOSITS (From player - Liability decreases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.LIABILITY,
                "PLAYER_DEPOSITS",
                amount + fee,
                0));

        // CREDIT: PLAYER_DEPOSITS (To player - Liability increases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.LIABILITY,
                "PLAYER_DEPOSITS",
                0,
                amount));

        // CREDIT: TRANSACTION_FEES (Revenue increases)
        if (fee > 0) {
            entries.add(new JournalEntry(
                    UUID.randomUUID(),
                    txId,
                    System.currentTimeMillis(),
                    JournalEntry.AccountType.REVENUE,
                    "TRANSACTION_FEES",
                    0,
                    fee));
        }

        persistenceService.saveJournalEntries(entries);
    }

    private void recordInterestJournal(UUID txId, long interest) {
        List<JournalEntry> entries = new ArrayList<>();

        // DEBIT: INTEREST_EXPENSE (Expense increases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.EXPENSE,
                "INTEREST_EXPENSE",
                interest,
                0));

        // CREDIT: PLAYER_DEPOSITS (Liability increases)
        entries.add(new JournalEntry(
                UUID.randomUUID(),
                txId,
                System.currentTimeMillis(),
                JournalEntry.AccountType.LIABILITY,
                "PLAYER_DEPOSITS",
                0,
                interest));

        persistenceService.saveJournalEntries(entries);
    }

    // ========== BACKGROUND TASKS ==========

    private void startInterestTask() {
        // Run every minute
        // Changed to synchronous to avoid race conditions with main thread transactions
        // and YAML persistence layer which is not thread-safe.
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                calculateAllInterest();
            } catch (Exception e) {
                plugin.getLogger().severe("[Bank] Error calculating interest: " + e.getMessage());
                e.printStackTrace();
            }
        }, (long) (20L * 60), plugin.getConfig().getLong("bank.interest_calc_interval_ticks", 1200));
    }
}
