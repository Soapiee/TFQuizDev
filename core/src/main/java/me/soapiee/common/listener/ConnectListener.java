package me.soapiee.common.listener;

import me.soapiee.common.SpectatorManager;
import me.soapiee.common.TFQuiz;
import me.soapiee.common.instance.Game;
import me.soapiee.common.manager.GameManager;
import me.soapiee.common.utils.PlayerCache;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectListener implements Listener {

    private final GameManager gameManager;
    private final PlayerCache playerCache;
    private final SpectatorManager specManager;

    public ConnectListener(TFQuiz main) {
        this.gameManager = main.getGameManager();
        this.playerCache = main.getPlayerCache();
        this.specManager = main.getSpecManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.gameManager.getEnforceLobbySpawn()) player.teleport(this.gameManager.getLobbySpawn());

        if (specManager.spectatorsExist()) specManager.updateTab(player);

        if (!player.hasPlayedBefore()) this.playerCache.addOfflinePlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Game game = this.gameManager.getGame(player);
        if (game != null) {
            if (game.isSpectator(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            if (game.isPhysicalArena()) {
                player.teleport(this.gameManager.getLobbySpawn());
            }
            game.removePlayer(player);
        }
    }
}
