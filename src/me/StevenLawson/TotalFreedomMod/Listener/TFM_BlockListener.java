package me.StevenLawson.TotalFreedomMod.Listener;

import me.StevenLawson.TotalFreedomMod.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.ItemStack;

public class TFM_BlockListener implements Listener
{
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event)
    {
        if (!TFM_ConfigEntry.ALLOW_FIRE_SPREAD.getBoolean())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event)
    {
        if (!TFM_ConfigEntry.ALLOW_FIRE_PLACE.getBoolean())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        Location block_pos = event.getBlock().getLocation();

        if (TFM_ConfigEntry.NUKE_MONITOR.getBoolean())
        {
            TFM_PlayerData playerdata = TFM_PlayerData.getPlayerData(player);

            Location player_pos = player.getLocation();

            final double nukeMonitorRange = TFM_ConfigEntry.NUKE_MONITOR_RANGE.getDouble().doubleValue();

            boolean out_of_range = false;
            if (!player_pos.getWorld().equals(block_pos.getWorld()))
            {
                out_of_range = true;
            }
            else if (player_pos.distanceSquared(block_pos) > (nukeMonitorRange * nukeMonitorRange))
            {
                out_of_range = true;
            }

            if (out_of_range)
            {
                if (playerdata.incrementAndGetFreecamDestroyCount() > TFM_ConfigEntry.FREECAM_TRIGGER_COUNT.getInteger())
                {
                    TFM_Util.bcastMsg(player.getName() + " has been flagged for possible freecam nuking.", ChatColor.RED);
                    TFM_Util.autoEject(player, "Freecam (extended range) block breaking is not permitted on this server.");

                    playerdata.resetFreecamDestroyCount();

                    event.setCancelled(true);
                    return;
                }
            }

            Long lastRan = TFM_Heartbeat.getLastRan();
            if (lastRan == null || lastRan + TotalFreedomMod.HEARTBEAT_RATE * 1000L < System.currentTimeMillis())
            {
                //TFM_Log.warning("Heartbeat service timeout - can't check block place/break rates.");
            }
            else
            {
                if (playerdata.incrementAndGetBlockDestroyCount() > TFM_ConfigEntry.NUKE_MONITOR_COUNT_BREAK.getInteger())
                {
                    TFM_Util.bcastMsg(player.getName() + " is breaking blocks too fast!", ChatColor.RED);
                    TFM_Util.autoEject(player, "You are breaking blocks too fast. Nukers are not permitted on this server.");

                    playerdata.resetBlockDestroyCount();

                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (TFM_ConfigEntry.PROTECTED_AREAS_ENABLED.getBoolean())
        {
            if (!TFM_SuperadminList.isUserSuperadmin(player))
            {
                if (TFM_ProtectedArea.isInProtectedArea(block_pos))
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRollbackBlockBreak(BlockBreakEvent event)
    {
        TFM_RollbackManager.blockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Player player = event.getPlayer();
        Location block_pos = event.getBlock().getLocation();

        if (TFM_ConfigEntry.NUKE_MONITOR.getBoolean())
        {
            TFM_PlayerData playerdata = TFM_PlayerData.getPlayerData(player);

            Location player_pos = player.getLocation();

            double nukeMonitorRange = TFM_ConfigEntry.NUKE_MONITOR_RANGE.getDouble().doubleValue();

            boolean out_of_range = false;
            if (!player_pos.getWorld().equals(block_pos.getWorld()))
            {
                out_of_range = true;
            }
            else if (player_pos.distanceSquared(block_pos) > (nukeMonitorRange * nukeMonitorRange))
            {
                out_of_range = true;
            }

            if (out_of_range)
            {
                if (playerdata.incrementAndGetFreecamPlaceCount() > TFM_ConfigEntry.FREECAM_TRIGGER_COUNT.getInteger())
                {
                    TFM_Util.bcastMsg(player.getName() + " has been flagged for possible freecam building.", ChatColor.RED);
                    TFM_Util.autoEject(player, "Freecam (extended range) block building is not permitted on this server.");

                    playerdata.resetFreecamPlaceCount();

                    event.setCancelled(true);
                    return;
                }
            }

            Long lastRan = TFM_Heartbeat.getLastRan();
            if (lastRan == null || lastRan + TotalFreedomMod.HEARTBEAT_RATE * 1000L < System.currentTimeMillis())
            {
                //TFM_Log.warning("Heartbeat service timeout - can't check block place/break rates.");
            }
            else
            {
                if (playerdata.incrementAndGetBlockPlaceCount() > TFM_ConfigEntry.NUKE_MONITOR_COUNT_PLACE.getInteger())
                {
                    TFM_Util.bcastMsg(player.getName() + " is placing blocks too fast!", ChatColor.RED);
                    TFM_Util.autoEject(player, "You are placing blocks too fast.");

                    playerdata.resetBlockPlaceCount();

                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (TFM_ConfigEntry.PROTECTED_AREAS_ENABLED.getBoolean())
        {
            if (!TFM_SuperadminList.isUserSuperadmin(player))
            {
                if (TFM_ProtectedArea.isInProtectedArea(block_pos))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        switch (event.getBlockPlaced().getType())
        {
            case LAVA:
            case STATIONARY_LAVA:
            {
                if (TFM_ConfigEntry.ALLOW_LAVA_PLACE.getBoolean())
                {
                    TFM_Log.info(String.format("%s placed lava @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Lava placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case WATER:
            case STATIONARY_WATER:
            {
                if (TFM_ConfigEntry.ALLOW_WATER_PLACE.getBoolean())
                {
                    TFM_Log.info(String.format("%s placed water @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Water placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case FIRE:
            {
                if (TFM_ConfigEntry.ALLOW_FIRE_PLACE.getBoolean())
                {
                    TFM_Log.info(String.format("%s placed fire @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));
                    player.sendMessage(ChatColor.GRAY + "Fire placement is currently disabled.");

                    event.setCancelled(true);
                }
                break;
            }
            case TNT:
            {
                if (TFM_ConfigEntry.ALLOW_EXPLOSIONS.getBoolean())
                {
                    TFM_Log.info(String.format("%s placed TNT @ %s", player.getName(), TFM_Util.formatLocation(event.getBlock().getLocation())));

                    player.getInventory().clear(player.getInventory().getHeldItemSlot());
                }
                else
                {
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), new ItemStack(Material.COOKIE, 1));

                    player.sendMessage(ChatColor.GRAY + "TNT is currently disabled.");
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRollbackBlockPlace(BlockPlaceEvent event)
    {
        TFM_RollbackManager.blockPlace(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        if (!TFM_ConfigEntry.ALLOW_FLIUD_SPREAD.getBoolean())
        {
            event.setCancelled(true);
        }
    }
}
