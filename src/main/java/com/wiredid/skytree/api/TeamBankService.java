package com.wiredid.skytree.api;

import java.util.UUID;

public interface TeamBankService {
    double getBalance(UUID islandId);

    void deposit(UUID playerId, UUID islandId, double amount);

    boolean withdraw(UUID playerId, UUID islandId, double amount);

    void reload();

    void save();
}
