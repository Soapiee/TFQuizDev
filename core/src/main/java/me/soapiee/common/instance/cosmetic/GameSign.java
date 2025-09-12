package me.soapiee.common.instance.cosmetic;

import me.soapiee.common.TFQuiz;
import me.soapiee.common.VersionManager;
import me.soapiee.common.instance.Game;
import me.soapiee.common.utils.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameSign {

    private final VersionManager versionManager;
    private final FileConfiguration config;
    private final String signID;
    private final Game game;
    private Sign signBlock;
    private Material material;
    private final Location location;
    private final HashMap<Integer, String> text = new HashMap<>();

    public GameSign(TFQuiz main, Game game, String signID) {
        versionManager = main.getVersionManager();
        config = main.getConfig();
        this.signID = signID;
        this.game = game;

        this.location = new Location(
                Bukkit.getWorld(config.getString("signs." + signID + ".world")),
                config.getDouble("signs." + signID + ".x"),
                config.getDouble("signs." + signID + ".y"),
                config.getDouble("signs." + signID + ".z"));


        String formatPath = ("signs." + signID + ".format");
        if (!config.isSet(formatPath)) {
            ArrayList<String> defaultFormat = new ArrayList<>();
            defaultFormat.add("%game_id%");
            defaultFormat.add(" ");
            defaultFormat.add("Edit me..");
            config.set(formatPath, defaultFormat);
            main.saveConfig();
        }

        List<String> lines = config.getStringList(formatPath);
        for (String string : lines) {
            text.put(lines.indexOf(string) + 1, string);
        }

        String materialPath = ("signs." + signID + ".material");
        if (!config.isSet(materialPath)) {
            config.set(materialPath, "OAK_WALL_SIGN");
            material = Material.OAK_WALL_SIGN;
            main.saveConfig();
        } else {
            material = Material.matchMaterial(config.getString(materialPath));
        }

        if (material == null || !material.name().contains("SIGN")) {
            material = Material.OAK_WALL_SIGN;
//            Utils.consoleMsg(ChatColor.RED + "Invalid sign material for sign " + signID + ". Defaulting to OAK_WALL_SIGN");
        }

        this.spawn();
    }

    public void spawn() {
        Block block = location.getBlock();
        if (block.getType() != material) block.setType(material);

        block = location.getBlock();
        String path = "signs." + signID + ".facing";
        BlockFace facing = BlockFace.valueOf(config.isSet(path) ? config.getString(path).toUpperCase() : "NORTH");

        if (block.getBlockData() instanceof WallSign) {
            Sign blockState = (Sign) block.getState();
            WallSign wallData = (WallSign) block.getBlockData();
            wallData.setFacing(facing);
            blockState.setBlockData(wallData);
            block.setBlockData(blockState.getBlockData());
        } else {
            org.bukkit.block.data.type.Sign signdata = (org.bukkit.block.data.type.Sign) block.getBlockData(); //1.20
            signdata.setRotation(facing);
            block.setBlockData(signdata);
        }

        signBlock = (Sign) block.getState();
//        this.signBlock.setWaxed(true);
        signBlock.getPersistentDataContainer().set(Keys.GAME_SIGN, PersistentDataType.STRING, signID);

        int i = 0;
        for (String line : text.values()) {
            if (line.contains("%")) {
                line = line.replace("%game_ID%", String.valueOf(game.getID()))
                        .replace("%game_state%", game.getStateDescription())
                        .replace("%game_players%", String.valueOf(game.getPlayingPlayers().size()))
                        .replace("%game_maxplayers%", String.valueOf(game.getMaxPlayers()));
            }

            versionManager.setText(signBlock, i, line);
            i++;
        }
        signBlock.update();
    }

    public void despawn() {
        location.getBlock().setType(Material.AIR);
    }

    public void update(String state) { //called when the game state changes
        for (int key : text.keySet()) {
            String line = text.get(key);
            if (line.contains("%")) {
                line = line.replace("%game_state%", state)
                        .replace("%game_ID%", String.valueOf(game.getID()))
                        .replace("%game_players%", String.valueOf(game.getPlayingPlayers().size()))
                        .replace("%game_maxplayers%", String.valueOf(game.getMaxPlayers()));
                versionManager.setText(signBlock, key - 1, line);
            }
        }
        signBlock.update();
    }

    public void update(int playerCount) { //called when the player count changes
        for (int key : text.keySet()) {
            String line = text.get(key);
            if (line.contains("%")) {
                line = line.replace("%game_ID%", String.valueOf(game.getID()))
                        .replace("%game_state%", game.getStateDescription())
                        .replace("%game_players%", String.valueOf(playerCount))
                        .replace("%game_maxplayers%", String.valueOf(game.getMaxPlayers()));
                versionManager.setText(signBlock, key - 1, line);
            }
        }
        signBlock.update();
    }

    public void update(int lineIndex, String line) { //called when the text is changed on an existing sign
        text.put(lineIndex + 1, line);

        if (line.contains("%")) {
            line = line.replace("%game_ID%", String.valueOf(game.getID()))
                    .replace("%game_state%", game.getStateDescription())
                    .replace("%game_players%", String.valueOf(game.getPlayingPlayers().size()))
                    .replace("%game_maxplayers%", String.valueOf(game.getMaxPlayers()));
        }
        versionManager.setText(signBlock, lineIndex, line);

        signBlock.update();
    }

    public String getID() {
        return signID;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public List<String> getText() {
        return new ArrayList<>(text.values());
    }
}
