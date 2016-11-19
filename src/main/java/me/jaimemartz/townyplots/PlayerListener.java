package me.jaimemartz.townyplots;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.*;
import me.jaimemartz.faucet.Messager;
import me.jaimemartz.townyplots.data.JsonLocation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerListener implements Listener {
    private final Set<WorldCoord> lockedCoords = ConcurrentHashMap.newKeySet();
    private final TownyPlots plugin;
    public PlayerListener(TownyPlots plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Messager msgr = new Messager(player);

        String[] parts = event.getMessage().substring(1).split("\\s+");
        if (parts[0].equalsIgnoreCase("plot") || parts[0].equalsIgnoreCase("towny:plot")) {
            Resident resident = TownyUtils.getResident(player);
            if (resident == null) return;

            Town town = TownyUtils.getTown(resident);
            if (town == null) return;

            TownBlock block = TownyUniverse.getTownBlock(player.getLocation());
            if (block == null) return;

            if (parts.length >= 2) {
                switch (parts[1].toLowerCase()) {
                    case "claim": {
                        if (!town.hasAssistant(resident) && !town.isMayor(resident)) {
                            if (town.hasTownBlock(block) && block.isForSale()) {
                                if (lockedCoords.contains(block.getWorldCoord())) {
                                    msgr.send("&cEsta parcela esta siendo comprada por otro jugador");
                                    event.setCancelled(true);
                                    break;
                                }

                                Set<WorldCoord> coords = new TownBlockDiscovery(block).getWorldCoords();
                                lockedCoords.addAll(coords);

                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    if (town.hasTownBlock(block) && block.isOwner(resident)) {

                                        if (coords.size() < 3) {
                                            msgr.send("&cNo se te han dado las parcelas adyacentes ya que alguna no está en venta");
                                            plugin.getLogger().severe(String.format("%s has tried to claim a plot in an unexpected place (%s) coll size %s",
                                                    player.getName(), block.getWorldCoord(), coords.size()
                                            ));
                                            return;
                                        }

                                        //Start claim task with all the coords
                                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                            Towny plugin = JavaPlugin.getPlugin(Towny.class);
                                            coords.forEach(coord -> {
                                                TownBlock other = TownyUtils.getBlock(coord);
                                                if (other != null) {
                                                    other.setPlotPrice(-1.0D);
                                                    other.setType(TownBlockType.RESIDENTIAL);
                                                    other.setResident(resident);

                                                    TownyUniverse.getDataSource().saveResident(resident);
                                                    TownyUniverse.getDataSource().saveTownBlock(other);
                                                    plugin.updateCache(coord);
                                                }
                                            });

                                            TownyUniverse.getDataSource().saveResident(resident); //Save the resident again, not sure why, towny does it.
                                            plugin.resetCache();

                                            //OLD METHOD:
                                            // new PlotClaim(JavaPlugin.getPlugin(Towny.class), player, resident, Lists.newArrayList(coords), true).start();

                                            lockedCoords.removeAll(coords);
                                            msgr.send("&bSe te han dado las parcelas adyacentes automáticamente");
                                        });
                                    }
                                }, 20 * 2);
                            }
                        } else {
                            msgr.send("&cEres un asistente o alcalde, por lo que no se te han dado las parcelas adyacentes");
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Messager msgr = new Messager(player);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (plugin.getDataPool().getSigns().containsValue(new JsonLocation(
                    block.getWorld().getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ())
            )) {
                Resident resident = TownyUtils.getResident(player);
                if (resident == null) return;

                Town town = TownyUtils.getTown(resident);
                if (town == null) return;

                TownBlock townBlock = TownyUniverse.getTownBlock(player.getLocation());
                if (townBlock == null) return;

                if (TownyUtils.townEquals(TownyUtils.getTown(resident), TownyUtils.getTown(townBlock))) {
                    List<TownBlock> blocks = resident.getTownBlocks();
                    if (blocks.size() == 0) {
                        msgr.send("&cNo tienes ninguna parcela de tu propiedad en esta ciudad");
                        return;
                    }

                    TownBlock target = blocks.get(ThreadLocalRandom.current().nextInt(blocks.size()));
                    if (target == null) return;

                    Location location = new Location(target.getWorldCoord().getBukkitWorld(),
                            target.getX() * 16,
                            64,
                            target.getZ() * 16);
                    player.teleport(location);
                    msgr.send("&aHas sido transportado a una de tus parcelas");
                } else {
                    msgr.send("&cEste cartel solo puede ser usado por residentes de esta ciudad");
                }
            }
        }
    }
}
