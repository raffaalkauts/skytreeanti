package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MachineService;

/**
 * Machine service implementation
 * NOTE: This service is currently not used. Machine logic is handled by
 * MachineProcessor.
 * This exists only to satisfy the interface requirement.
 * See: com.wiredid.skytree.machine.MachineProcessor for actual machine logic.
 */
public class SkytreeMachineService implements MachineService {

    public SkytreeMachineService(SkytreePlugin plugin) {
        plugin.getLogger().info("MachineService initialized (logic handled by MachineProcessor)");
    }
}
