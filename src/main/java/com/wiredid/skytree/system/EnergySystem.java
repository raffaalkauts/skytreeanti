package com.wiredid.skytree.system;

import org.bukkit.Location;

import java.util.*;

/**
 * Energy System (Forge Energy compatible)
 * Tracks FE storage and transfer
 */
public class EnergySystem {

    // Energy storage per location
    private final Map<Location, EnergyStorage> energyBlocks = new HashMap<>();

    // Cable network connections
    private final Map<Location, Set<Location>> cableConnections = new HashMap<>();

    public static class EnergyStorage {
        private int current;
        private final int capacity;
        private final int maxReceive;
        private final int maxExtract;

        public EnergyStorage(int capacity, int maxTransfer) {
            this(capacity, maxTransfer, maxTransfer);
        }

        public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
            this.current = 0;
        }

        public int receiveEnergy(int amount, boolean simulate) {
            int received = Math.min(capacity - current, Math.min(maxReceive, amount));
            if (!simulate) {
                current += received;
            }
            return received;
        }

        public int extractEnergy(int amount, boolean simulate) {
            int extracted = Math.min(current, Math.min(maxExtract, amount));
            if (!simulate) {
                current -= extracted;
            }
            return extracted;
        }

        public int getEnergyStored() {
            return current;
        }

        public int getMaxEnergyStored() {
            return capacity;
        }

        public boolean canReceive() {
            return maxReceive > 0;
        }

        public boolean canExtract() {
            return maxExtract > 0;
        }

        public void setEnergy(int energy) {
            this.current = Math.min(energy, capacity);
        }
    }

    // Register an energy block
    public void registerEnergyBlock(Location location, int capacity, int maxTransfer) {
        energyBlocks.put(location, new EnergyStorage(capacity, maxTransfer));
    }

    public void registerGenerator(Location location, int capacity, int production) {
        energyBlocks.put(location, new EnergyStorage(capacity, 0, production));
    }

    public void registerBattery(Location location, int capacity) {
        energyBlocks.put(location, new EnergyStorage(capacity, 1000, 1000));
    }

    public void registerMachine(Location location, int capacity, int consumption) {
        energyBlocks.put(location, new EnergyStorage(capacity, consumption, 0));
    }

    // Remove energy block
    public void unregisterEnergyBlock(Location location) {
        energyBlocks.remove(location);
        cableConnections.remove(location);
        // Remove from other connections
        for (Set<Location> connections : cableConnections.values()) {
            connections.remove(location);
        }
    }

    // Cable connections
    public void connectCable(Location from, Location to) {
        cableConnections.putIfAbsent(from, new HashSet<>());
        cableConnections.putIfAbsent(to, new HashSet<>());
        cableConnections.get(from).add(to);
        cableConnections.get(to).add(from);
    }

    public void disconnectCable(Location location) {
        Set<Location> connections = cableConnections.remove(location);
        if (connections != null) {
            for (Location connected : connections) {
                cableConnections.get(connected).remove(location);
            }
        }
    }

    // Get energy storage
    public EnergyStorage getEnergyStorage(Location location) {
        return energyBlocks.get(location);
    }

    public boolean hasEnergy(Location location) {
        return energyBlocks.containsKey(location);
    }

    // Transfer energy through network
    public void transferEnergy() {
        for (Map.Entry<Location, EnergyStorage> entry : energyBlocks.entrySet()) {
            Location loc = entry.getKey();
            EnergyStorage storage = entry.getValue();

            if (!storage.canExtract() || storage.getEnergyStored() == 0)
                continue;

            Set<Location> connections = cableConnections.get(loc);
            if (connections == null || connections.isEmpty())
                continue;

            // Calculate transfer per connection
            int availableEnergy = storage.extractEnergy(Integer.MAX_VALUE, true);
            int perConnection = availableEnergy / connections.size();

            for (Location target : connections) {
                EnergyStorage targetStorage = energyBlocks.get(target);
                if (targetStorage == null || !targetStorage.canReceive())
                    continue;

                // Transfer energy
                int transferred = storage.extractEnergy(perConnection, false);
                targetStorage.receiveEnergy(transferred, false);
            }
        }
    }

    // Generate energy for generators
    public void generateEnergy(Location location, int amount) {
        EnergyStorage storage = energyBlocks.get(location);
        if (storage != null) {
            storage.receiveEnergy(amount, false);
        }
    }

    // Consume energy for machines
    public boolean consumeEnergy(Location location, int amount) {
        EnergyStorage storage = energyBlocks.get(location);
        if (storage == null)
            return false;

        int extracted = storage.extractEnergy(amount, false);
        return extracted >= amount;
    }

    // Get energy info
    public String getEnergyInfo(Location location) {
        EnergyStorage storage = energyBlocks.get(location);
        if (storage == null)
            return "§cNo energy storage";

        int current = storage.getEnergyStored();
        int max = storage.getMaxEnergyStored();
        double percentage = (double) current / max * 100;

        return String.format("§e%,d §7/ §e%,d FE §7(%.1f%%)", current, max, percentage);
    }

    // Get all energy blocks
    public Set<Location> getAllEnergyBlocks() {
        return new HashSet<>(energyBlocks.keySet());
    }
}

