package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Bounty;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BountyService {
    void addBounty(UUID target, UUID issuer, double amount);

    Optional<Bounty> getBounty(UUID target);

    List<Bounty> getAllBounties();

    List<Bounty> getTopBounties(int limit);

    boolean hasBounty(UUID target);

    void removeBounty(UUID target); // Claim or remove
}
