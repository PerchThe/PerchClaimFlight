package me.perch.claimflight;

import me.perch.claimflight.api.ClaimChecker;
import me.perch.claimflight.api.ClaimFlight;
import me.perch.claimflight.checker.GPChecker;
import me.perch.claimflight.listeners.EssentialsListener;
import me.perch.claimflight.listeners.FlyCommandListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public class ClaimFlightImpl extends JavaPlugin implements ClaimFlight, Listener {

  private boolean permissionRequired;
  private boolean otherTrustedClaims;
  private boolean adminClaims;
  private boolean freeWorld;
  private boolean drop;
  private boolean gamemodeBypass;
  private boolean onFlyCommand;

  private String configNotAllowed;
  private String configFlightDisabled;

  private List<ClaimChecker> checkers = new ArrayList<>();
  private Listener flyListener;

  // Track players who just lost flight to prevent fall damage
  private final Set<UUID> justLostFlight = new HashSet<>();

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    getConfig().options().copyDefaults(true);
    saveConfig();
    load();

    if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
      getLogger().info("Hooking into GriefPrevention.");
      checkers.add(new GPChecker(this));
    }

    if (checkers.isEmpty()) {
      getLogger().warning("No claim plugin found. Supported: GriefPrevention");
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  private void load() {
    permissionRequired = getConfig().getBoolean("permissionRequired", false);
    otherTrustedClaims = getConfig().getBoolean("otherTrustedClaims", true);
    adminClaims = getConfig().getBoolean("adminClaims", true);
    freeWorld = getConfig().getBoolean("freeWorld", false);
    drop = getConfig().getBoolean("drop", false);
    gamemodeBypass = getConfig().getBoolean("gamemodeBypass", true);
    onFlyCommand = getConfig().getBoolean("onFlyCommand", false);
    configNotAllowed = ChatColor.translateAlternateColorCodes('&', getConfig().getString("notAllowed"));
    configFlightDisabled = ChatColor.translateAlternateColorCodes('&', getConfig().getString("flightDisabled"));
    if ((flyListener == null) == onFlyCommand) {
      if (flyListener == null) {
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
          flyListener = new EssentialsListener(this);
        } else {
          flyListener = new FlyCommandListener(this);
        }
      } else {
        HandlerList.unregisterAll(flyListener);
        flyListener = null;
      }
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("rl")) {
      reloadConfig();
      load();
      sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
      return true;
    }

    sender.sendMessage(ChatColor.BLUE + "ClaimFlight by Trophonix...");
    sender.sendMessage(ChatColor.AQUA + "/ClaimFlight rl " + ChatColor.WHITE + "Reload the config.");
    return true;
  }

  public boolean canBypass(Player player) {
    if ((gamemodeBypass && canFly(player.getGameMode()))) return true;
    if (player.hasPermission("ClaimFlight.bypass")) return true;
    return false;
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (event.getTo() == null) return;
    Player player = event.getPlayer();
    if (!player.isFlying()) return;
    if (canBypass(player)) return;
    if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY()) {
      return;
    }

    // Only block movement and show message, do NOT disable flight here
    if (!isAllowedToFly(event.getTo(), player)) {
      event.setCancelled(true);
      player.sendMessage(configFlightDisabled);
    }
  }

  @EventHandler
  public void onFlyChange(PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();
    if (!event.isFlying()) return;
    if (canBypass(player)) return;
    if (!isAllowedToFly(player)) {
      event.setCancelled(true);
      player.setAllowFlight(false);
      player.setFlying(false);
      player.setFallDistance(0);
      justLostFlight.add(player.getUniqueId());
      player.sendMessage(configNotAllowed);
    }
  }

  @EventHandler
  public void onFallDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player) {
      Player player = (Player) event.getEntity();
      if (event.getCause() == EntityDamageEvent.DamageCause.FALL && justLostFlight.remove(player.getUniqueId())) {
        event.setCancelled(true);
      }
    }
  }

  public boolean isAllowedToFly(Location to, Player player) {
    if (permissionRequired && !player.hasPermission("ClaimFlight.fly")) return false;
    for (int i = 0; i < checkers.size(); i++) {
      ClaimChecker checker = checkers.get(i);
      if (otherTrustedClaims) {
        if (checker.isInTrustedClaim(player, to)) {
          return true;
        }
      } else {
        if (checker.isInOwnClaim(player, to)) {
          return true;
        }
      }
    }
    return isFreeWorld();
  }

  public boolean isAllowedToFly(Player player) {
    return isAllowedToFly(player.getLocation(), player);
  }

  public boolean canFly(GameMode gamemode) {
    return gamemode == GameMode.CREATIVE || gamemode == GameMode.SPECTATOR;
  }

  // --- Implement required interface methods ---
  @Override
  public boolean isAdminClaims() {
    return adminClaims;
  }

  @Override
  public boolean isFreeWorld() {
    return freeWorld;
  }

  // --- Explicit getter for configNotAllowed, in case Lombok is not working ---
  public String getConfigNotAllowed() {
    return configNotAllowed;
  }
}