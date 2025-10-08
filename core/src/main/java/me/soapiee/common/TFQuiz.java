package me.soapiee.common;

import me.soapiee.common.command.AdminCommand;
import me.soapiee.common.command.PlayerCommand;
import me.soapiee.common.hooks.PlaceHolderAPIHook;
import me.soapiee.common.hooks.VaultHook;
import me.soapiee.common.instance.Game;
import me.soapiee.common.instance.cosmetic.GameSign;
import me.soapiee.common.listener.ChatListener;
import me.soapiee.common.listener.ConnectListener;
import me.soapiee.common.listener.PlayerListener;
import me.soapiee.common.manager.GameManager;
import me.soapiee.common.manager.MessageManager;
import me.soapiee.common.utils.Logger;
import me.soapiee.common.utils.PlayerCache;
import me.soapiee.common.utils.UpdateChecker;
import me.soapiee.common.utils.Utils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TFQuiz extends JavaPlugin {

    //TODO: Add "addGame" + "removeGame" command functionality
    //TODO: Right click signs to edit them (in PlayerListener)
    //TODO: Add question categories
    //TODO:

    private GameManager gameManager;
    private MessageManager messageManager;
    private SpectatorManager specManager;
    private VersionManager versionManager;
    private PlayerCache playerCache;
    private PlayerListener playerListener;
    private VaultHook vaultHook;
    private Logger logger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        logger = new Logger(this);
        messageManager = new MessageManager(this);
        specManager = new SpectatorManager(this);
        versionManager = new VersionManager(logger);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) new PlaceHolderAPIHook(this).register();
        if (getServer().getPluginManager().getPlugin("Vault") != null) vaultHook = new VaultHook();
        new Metrics(this, 25563);

        if (!getConfig().isConfigurationSection("games") || getConfig().getConfigurationSection("games").getKeys(false).isEmpty()) {
            Utils.consoleMsg(ChatColor.RED + "Please set up some games in the config.yml");
//            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        gameManager = new GameManager(this);
        playerCache = new PlayerCache();

        playerListener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(new ConnectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        getCommand("tf").setExecutor(new AdminCommand(this));
        getCommand("game").setExecutor(new PlayerCommand(this));

        UpdateChecker updateChecker = new UpdateChecker(this, 125077);
        updateChecker.updateAlert(this);
    }

    @Override
    public void onDisable() {
        if (gameManager == null) return;

        for (Game game : gameManager.getGames()) {
            for (Player player : game.getAllPlayers()) {
                if (game.isSpectator(player)) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                game.restoreInventory(player);

                if (game.isPhysicalArena()) player.teleport(getGameManager().getLobbySpawn());
            }
            if (game.getHologram() != null) game.getHologram().despawn();

            gameManager.killOtherHolos();

            for (GameSign sign : game.getSigns()) {
                sign.despawn();
            }
        }
    }

    public SpectatorManager getSpecManager() {
        return specManager;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public VaultHook getVaultHook() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
        else return vaultHook;
    }

    public Logger getCustomLogger() {
        return logger;
    }
}
