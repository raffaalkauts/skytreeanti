package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;

public class WorthTooltipPacketSystem {

    @SuppressWarnings("unused")
    private final SkytreePlugin plugin;
    private boolean enabled = true;

    public WorthTooltipPacketSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
