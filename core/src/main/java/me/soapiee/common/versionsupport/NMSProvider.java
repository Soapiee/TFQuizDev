package me.soapiee.common.versionsupport;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public interface NMSProvider {

    boolean setSpectator(Player player);

    void unSetSpectator(Player player);

    void updateTab(Player player, HashSet<UUID> spectators);
}
