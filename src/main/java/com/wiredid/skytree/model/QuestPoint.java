package com.wiredid.skytree.model;

/**
 * Simple wrapper for Quest Points currency
 */
public class QuestPoint {
    private int balance;

    public QuestPoint() {
        this.balance = 0;
    }

    public QuestPoint(int initialBalance) {
        this.balance = initialBalance;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void add(int amount) {
        this.balance += amount;
    }

    public boolean subtract(int amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public boolean has(int amount) {
        return balance >= amount;
    }
}
