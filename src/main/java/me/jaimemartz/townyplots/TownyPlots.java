package me.jaimemartz.townyplots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import me.jaimemartz.faucet.Messager;
import me.jaimemartz.townyplots.data.JsonDataPool;
import me.jaimemartz.townyplots.data.JsonLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.Set;

public final class TownyPlots extends JavaPlugin {
    public static int SAVE_INTERVAL = 10;
    private Gson gson;
    private JsonDataPool database;

    @Override
    public void onEnable() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.enableComplexMapKeySerialization();
        gson = builder.create();

        //Loading database
        File file = new File(getDataFolder(), "data.json");
        if (file.exists()) {
            getLogger().info("Database exists, reading data...");
            try (JsonReader reader = new JsonReader(new FileReader(file))) {
                database = gson.fromJson(reader, JsonDataPool.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            getLogger().fine("Database does not exist, it will be created on server shutdown");
            database = new JsonDataPool();
        }

        //Database save task
        getLogger().info(String.format("The database will be saved every %s minutes", SAVE_INTERVAL));
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().info("Periodically saving database...");
                saveDatabase();
            }
        }.runTaskTimerAsynchronously(this, SAVE_INTERVAL * 60 * 20, SAVE_INTERVAL * 60 * 20);

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            getLogger().info("Saving database...");
            saveDatabase();
        } else {
            getLogger().info("Database is null, not saving database...");
        }
    }

    private void saveDatabase() {
        try (Writer writer = new FileWriter(new File(getDataFolder(), "data.json"))) {
            String output = gson.toJson(database, JsonDataPool.class);
            writer.write(output);
        } catch (IOException e) {
            getLogger().severe("Something went terribly wrong, couldn't save the database");
            e.printStackTrace();
        }
    }

    /*
    public Set<TownBlock> getAdjacentSellingBlocks(TownBlock block) {
        Set<TownBlock> results = new HashSet<>();
        while (results.size() < 3) {
            for (TownBlock other : this.getAdjacentBlocks(block)) {
                if (other != null && other.isForSale()) {
                    results.add(other);
                    block = other;
                }
            }
        }
        return results;
    }

    public TownBlock[] getAdjacentBlocks(TownBlock block) {
        WorldCoord coord = block.getWorldCoord();
        return new TownBlock[] {
                TownyUtils.getBlock(coord.add(1, 0)),
                TownyUtils.getBlock(coord.add(-1, 0)),
                TownyUtils.getBlock(coord.add(0, 1)),
                TownyUtils.getBlock(coord.add(0, -1))
        };
    }
    */

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Messager msgr = new Messager(sender);
        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "addsign": {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 2) {
                            String name = args[1];
                            Block block = player.getTargetBlock((Set<Material>) null, 5);
                            if (block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
                                JsonLocation location = new JsonLocation(
                                        block.getWorld().getName(),
                                        block.getX(),
                                        block.getY(),
                                        block.getZ()
                                );

                                if (database.getSigns().containsKey(name)) {
                                    msgr.send("&cThere is an sign already registered with that name");
                                    return true;
                                }

                                if (database.getSigns().containsValue(location)) {
                                    msgr.send("&cThat block is already registered");
                                    return true;
                                }

                                database.getSigns().put(name, location);
                                msgr.send("&aSuccessfully added that sign");
                            } else {
                                msgr.send("&cYou are not looking at an sign");
                            }
                        } else break;
                    } else {
                        msgr.send("&cThis command can only be executed by a player");
                    }
                    return true;
                }
                case "removesign": {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 2) {
                            String name = args[1];
                            if (database.getSigns().remove(name) != null) {
                                msgr.send(String.format("&aSuccessfully removed the sign named %s", name));
                            } else {
                                msgr.send(String.format("&cThere is no sign named %s", name));
                            }
                        } else break;
                    } else {
                        msgr.send("&cThis command can only be executed by a player");
                    }
                    return true;
                }
            }
        }

        msgr.send(
                "&e=====================================================",
                "&7Commands for TownyPlots:",
                "&3/.. addsign <id> &7- &cAdds the sign you are looking at to the plugin",
                "&3/.. removesign <id> &7- &cRemoves the sign registered with that name",
                "&e====================================================="
        );
        return true;
    }

    public JsonDataPool getDataPool() {
        return database;
    }
}
