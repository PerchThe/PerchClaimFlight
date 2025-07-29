package me.perch.claimflight.listeners;

import me.perch.claimflight.ClaimFlightImpl;
import net.ess3.api.events.FlyStatusChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EssentialsListener implements Listener {

  private ClaimFlightImpl pl;

  public EssentialsListener(ClaimFlightImpl pl) {
    this.pl = pl;
    pl.getServer().getPluginManager().registerEvents(this, pl);
  }

  @EventHandler
  public void onEssentialsFly(FlyStatusChangeEvent event) {
    if (!event.getValue()) return;
    Player player = event.getAffected().getBase();
    if (pl.canBypass(player)) return;
    if (!pl.isAllowedToFly(player)) {
      event.setCancelled(true);
      player.sendMessage(pl.getConfigNotAllowed());
    }
  }

}
