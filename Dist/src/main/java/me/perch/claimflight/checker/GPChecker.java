package me.perch.claimflight.checker;

import me.perch.claimflight.api.ClaimChecker;
import me.perch.claimflight.api.ClaimFlight;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GPChecker implements ClaimChecker {

  private final ClaimFlight pl;

  // Explicit constructor (Lombok not required)
  public GPChecker(ClaimFlight pl) {
    this.pl = pl;
  }

  private Claim getClaim(Location loc) {
    return GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);
  }

  @Override
  public boolean isInOwnClaim(Player player, Location loc) {
    Claim claim = getClaim(loc);
    if (claim == null) return false;
    if (claim.isAdminClaim()) return pl.isAdminClaims();
    else return player.getUniqueId().equals(claim.ownerID);
  }

  @Override
  public boolean isInTrustedClaim(Player player, Location loc) {
    Claim claim = getClaim(loc);
    if (claim == null) return false;
    if (claim.isAdminClaim()) return pl.isAdminClaims();
    else return claim.allowContainers(player) == null;
  }
}