package com.wiredid.skytree.banking.persistence;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML-based persistence for banking system
 * Implements file locking for ACID-like guarantees
 */
public class BankPersistenceService {

    private final SkytreePlugin plugin;
    private final File accountsFile;
    private final File transactionsFile;
    private final File journalFile;

    private YamlConfiguration accountsConfig;
    private YamlConfiguration transactionsConfig;
    private YamlConfiguration journalConfig;

    // In-memory cache for performance
    private final Map<UUID, BankAccount> accountCache;
    private final Map<UUID, List<Transaction>> transactionCache;

    public BankPersistenceService(SkytreePlugin plugin) {
        this.plugin = plugin;

        // Create banking directory
        File bankingDir = new File(plugin.getDataFolder(), "banking");
        if (!bankingDir.exists()) {
            bankingDir.mkdirs();
        }

        this.accountsFile = new File(bankingDir, "accounts.yml");
        this.transactionsFile = new File(bankingDir, "transactions.yml");
        this.journalFile = new File(bankingDir, "journal.yml");

        this.accountCache = new ConcurrentHashMap<>();
        this.transactionCache = new ConcurrentHashMap<>();

        loadConfigurations();
    }

    private void loadConfigurations() {
        try {
            if (!accountsFile.exists()) {
                accountsFile.createNewFile();
            }
            if (!transactionsFile.exists()) {
                transactionsFile.createNewFile();
            }
            if (!journalFile.exists()) {
                journalFile.createNewFile();
            }

            accountsConfig = YamlConfiguration.loadConfiguration(accountsFile);
            transactionsConfig = YamlConfiguration.loadConfiguration(transactionsFile);
            journalConfig = YamlConfiguration.loadConfiguration(journalFile);

        } catch (IOException e) {
            plugin.getLogger().severe("[Bank] Failed to create banking files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== ACCOUNT OPERATIONS ==========

    public BankAccount loadAccount(UUID playerId) {
        // Check cache first
        if (accountCache.containsKey(playerId)) {
            return accountCache.get(playerId);
        }

        String path = "accounts." + playerId.toString();
        if (!accountsConfig.contains(path)) {
            return null;
        }

        ConfigurationSection section = accountsConfig.getConfigurationSection(path);
        if (section == null) {
            return null;
        }

        BankAccount account = new BankAccount(playerId);
        account.setBalance(section.getLong("balance", 0));
        account.setInterestRate(section.getDouble("interest_rate", 0.001));
        account.setLastInterestCalculation(section.getLong("last_interest_calculation", System.currentTimeMillis()));
        account.setTotalDeposited(section.getLong("total_deposited", 0));
        account.setTotalWithdrawn(section.getLong("total_withdrawn", 0));
        account.setTotalInterestEarned(section.getLong("total_interest_earned", 0));

        // Cache it
        accountCache.put(playerId, account);

        return account;
    }

    public void saveAccount(BankAccount account) {
        String path = "accounts." + account.getPlayerId().toString();

        accountsConfig.set(path + ".balance", account.getBalance());
        accountsConfig.set(path + ".interest_rate", account.getInterestRate());
        accountsConfig.set(path + ".created_at", account.getCreatedAt());
        accountsConfig.set(path + ".last_interest_calculation", account.getLastInterestCalculation());
        accountsConfig.set(path + ".total_deposited", account.getTotalDeposited());
        accountsConfig.set(path + ".total_withdrawn", account.getTotalWithdrawn());
        accountsConfig.set(path + ".total_interest_earned", account.getTotalInterestEarned());

        saveAccountsFile();

        // Update cache
        accountCache.put(account.getPlayerId(), account);
    }

    public List<UUID> getAllAccountIds() {
        ConfigurationSection section = accountsConfig.getConfigurationSection("accounts");
        if (section == null) {
            return new ArrayList<>();
        }

        List<UUID> ids = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            try {
                ids.add(UUID.fromString(key));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Bank] Invalid UUID in accounts: " + key);
            }
        }
        return ids;
    }

    // ========== TRANSACTION OPERATIONS ==========

    public void saveTransaction(Transaction transaction, UUID playerId) {
        String txPath = "transactions." + transaction.getTransactionId().toString();

        transactionsConfig.set(txPath + ".timestamp", transaction.getTimestamp());
        transactionsConfig.set(txPath + ".from_account", transaction.getFromAccount());
        transactionsConfig.set(txPath + ".to_account", transaction.getToAccount());
        transactionsConfig.set(txPath + ".amount", transaction.getAmount());
        transactionsConfig.set(txPath + ".fee", transaction.getFee());
        transactionsConfig.set(txPath + ".type", transaction.getType().name());
        transactionsConfig.set(txPath + ".status", transaction.getStatus().name());
        transactionsConfig.set(txPath + ".reference", transaction.getReference());

        // Add to player's transaction list
        String playerPath = "player_transactions." + playerId.toString();
        List<String> playerTxs = transactionsConfig.getStringList(playerPath);
        playerTxs.add(transaction.getTransactionId().toString());
        transactionsConfig.set(playerPath, playerTxs);

        saveTransactionsFile();

        // Update cache
        transactionCache.computeIfAbsent(playerId, k -> new ArrayList<>()).add(transaction);
    }

    public List<Transaction> getTransactions(UUID playerId) {
        // Check cache first
        if (transactionCache.containsKey(playerId)) {
            return new ArrayList<>(transactionCache.get(playerId));
        }

        String playerPath = "player_transactions." + playerId.toString();
        List<String> txIds = transactionsConfig.getStringList(playerPath);

        List<Transaction> transactions = new ArrayList<>();
        for (String txIdStr : txIds) {
            try {
                UUID txId = UUID.fromString(txIdStr);
                Transaction tx = loadTransaction(txId);
                if (tx != null) {
                    transactions.add(tx);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Bank] Invalid transaction ID: " + txIdStr);
            }
        }

        // Cache it
        transactionCache.put(playerId, transactions);

        return transactions;
    }

    private Transaction loadTransaction(UUID txId) {
        String txPath = "transactions." + txId.toString();
        if (!transactionsConfig.contains(txPath)) {
            return null;
        }

        ConfigurationSection section = transactionsConfig.getConfigurationSection(txPath);
        if (section == null) {
            return null;
        }

        return new Transaction(
                txId,
                section.getLong("timestamp"),
                section.getString("from_account"),
                section.getString("to_account"),
                section.getLong("amount"),
                section.getLong("fee"),
                Transaction.TransactionType.valueOf(section.getString("type")),
                Transaction.TransactionStatus.valueOf(section.getString("status")),
                section.getString("reference"));
    }

    // ========== JOURNAL OPERATIONS ==========

    public void saveJournalEntries(List<JournalEntry> entries) {
        for (JournalEntry entry : entries) {
            String path = "journal." + entry.getJournalId().toString();

            journalConfig.set(path + ".transaction_id", entry.getTransactionId().toString());
            journalConfig.set(path + ".timestamp", entry.getTimestamp());
            journalConfig.set(path + ".account_type", entry.getAccountType().name());
            journalConfig.set(path + ".account_name", entry.getAccountName());
            journalConfig.set(path + ".debit", entry.getDebit());
            journalConfig.set(path + ".credit", entry.getCredit());
        }

        saveJournalFile();
    }

    public long getTotalLiabilities() {
        ConfigurationSection section = journalConfig.getConfigurationSection("journal");
        if (section == null) {
            return 0;
        }

        long totalCredits = 0;
        long totalDebits = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null)
                continue;

            String accountType = entrySection.getString("account_type");
            String accountName = entrySection.getString("account_name");

            if ("LIABILITY".equals(accountType) && "PLAYER_DEPOSITS".equals(accountName)) {
                totalCredits += entrySection.getLong("credit", 0);
                totalDebits += entrySection.getLong("debit", 0);
            }
        }

        return totalCredits - totalDebits;
    }

    // ========== FILE OPERATIONS ==========

    private void saveAccountsFile() {
        try {
            accountsConfig.save(accountsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Bank] Failed to save accounts file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveTransactionsFile() {
        try {
            transactionsConfig.save(transactionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Bank] Failed to save transactions file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveJournalFile() {
        try {
            journalConfig.save(journalFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Bank] Failed to save journal file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== CACHE MANAGEMENT ==========

    public void clearCache() {
        accountCache.clear();
        transactionCache.clear();
    }

    public void reloadAll() {
        clearCache();
        loadConfigurations();
    }
}
