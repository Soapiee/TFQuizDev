package me.soapiee.common.versionsupport;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

public class TabUpdate extends BukkitRunnable {

    private final NMSProvider provider;
    private final Player player;
    private final HashSet<UUID> spectators;

    public TabUpdate(NMSProvider provider, Player player, HashSet<UUID> spectators) {
        this.provider = provider;
        this.player = player;
        this.spectators = spectators;
    }

    @Override
    public void run() {
        provider.updateTab(player, spectators);
        this.cancel();
    }
}
