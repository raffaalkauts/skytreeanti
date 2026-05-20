package com.wiredid.skytree.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobData {

    private final UUID playerId;
    private final ConcurrentHashMap<String, JobProgress> jobs;

    public JobData(UUID playerId) {
        this.playerId = playerId;
        this.jobs = new ConcurrentHashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ConcurrentHashMap<String, JobProgress> getJobs() {
        return jobs;
    }

    public JobProgress getOrCreate(String jobId) {
        return jobs.computeIfAbsent(jobId, k -> new JobProgress(jobId));
    }

    public int getLevel(String jobId) {
        JobProgress p = jobs.get(jobId);
        return p != null ? p.level : 0;
    }

    public double getXp(String jobId) {
        JobProgress p = jobs.get(jobId);
        return p != null ? p.xp : 0;
    }

    public double getTotalEarned(String jobId) {
        JobProgress p = jobs.get(jobId);
        return p != null ? p.totalEarned : 0;
    }

    public void addXp(String jobId, double amount) {
        JobProgress p = getOrCreate(jobId);
        p.xp += amount;
    }

    public void addEarned(String jobId, double amount) {
        JobProgress p = getOrCreate(jobId);
        p.totalEarned += amount;
    }

    public void setLevel(String jobId, int level) {
        JobProgress p = getOrCreate(jobId);
        p.level = level;
        p.xp = 0;
    }

    public static class JobProgress {
        public String jobId;
        public int level;
        public double xp;
        public double totalEarned;
        public int actions;

        public JobProgress() {}

        public JobProgress(String jobId) {
            this.jobId = jobId;
            this.level = 0;
            this.xp = 0;
            this.totalEarned = 0;
            this.actions = 0;
        }

        public JobProgress(String jobId, int level, double xp, double totalEarned, int actions) {
            this.jobId = jobId;
            this.level = level;
            this.xp = xp;
            this.totalEarned = totalEarned;
            this.actions = actions;
        }
    }
}
