package me.soapiee.common;

import me.soapiee.common.versionsupport.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class SpectatorManager {

    private NMSProvider provider;
    private final TFQuiz main;
    private final HashSet<UUID> spectators;

    public SpectatorManager(TFQuiz main) {
        this.main = main;
        this.spectators = new HashSet<>();

        try {
            String packageName = SpectatorManager.class.getPackage().getName();
            String version = "v" + Bukkit.getBukkitVersion().split("-")[0].replace(".", "_");
            String providerName = NMSVersion.valueOf(version).getPackage();
//            Utils.consoleMsg(ChatColor.GREEN + providerName);
            provider = (NMSProvider) Class.forName(packageName + "." + providerName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 ClassCastException | IllegalArgumentException ex) {
//            ex.printStackTrace();
            Bukkit.getLogger().log(Level.SEVERE, "[TFQuiz] Unsupported NMS version detected. The Spectator system will be diminished. Its recommended that you disable it in the config");
            provider = new NMS_Unsupported();
        }
    }

//    public boolean isPaper() {
//        try {
//            Class.forName("com.destroystokyo.paper.ClientOption");
//            return true;
//        } catch (ClassNotFoundException ignored) {
//        }
//
//        return false;
//    }

    public boolean setSpectator(Player player) {
        if (provider.setSpectator(player)) {
            this.spectators.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void unSetSpectator(Player player) {
        provider.unSetSpectator(player);
        this.spectators.remove(player.getUniqueId());
        new GamemodeChange(player).runTaskLater(main, 1);
    }

    public boolean spectatorsExist() {
        return !this.spectators.isEmpty();
    }

    public void updateTab(Player player) {
        new TabUpdate(provider, player, spectators).runTaskLater(main, 10);
    }
}
