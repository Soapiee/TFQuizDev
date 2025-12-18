package me.soapiee.common.instance;

import me.soapiee.common.SpectatorManager;
import me.soapiee.common.TFQuiz;
import me.soapiee.common.enums.GameState;
import me.soapiee.common.enums.Message;
import me.soapiee.common.instance.cosmetic.GameSign;
import me.soapiee.common.instance.cosmetic.Hologram;
import me.soapiee.common.instance.logic.Countdown;
import me.soapiee.common.instance.logic.Procedure;
import me.soapiee.common.instance.logic.TeleportTask;
import me.soapiee.common.manager.MessageManager;
import me.soapiee.common.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Game {

    private final GameSpecs specs;
    private final TFQuiz main;
    private final MessageManager messageManager;
    private final SpectatorManager specManager;

    private GameState state;
    private final boolean physicalArena;
    private final boolean enforceSurvival;
    private boolean forceStart;
    private boolean broadcastWinners;
    private Location spawn;
    private Hologram hologram;
    private final ArrayList<GameSign> signs = new ArrayList<>();

    private final HashSet<Player> playingPlayers;
    private final HashMap<UUID, ItemStack[]> inventories;
    private final HashSet<Player> spectators;
    private final HashSet<Player> allPlayers;
    private Countdown countdown;
    private Procedure procedure;

    public Game(TFQuiz main, GameSpecs specs) {
        this.specs = specs;
        this.main = main;
        this.messageManager = main.getMessageManager();
        this.specManager = main.getSpecManager();

        this.state = specs.getInitialState();
        this.physicalArena = specs.isPhysicalArena();
        this.forceStart = false;
        this.broadcastWinners = specs.broadcastWinners();
        this.enforceSurvival = specs.enforceSurvival();

        this.allPlayers = new HashSet<>();
        this.playingPlayers = new HashSet<>();
        this.inventories = new HashMap<>();
        this.countdown = new Countdown(this.main, this, specs.getCountdownSeconds());
        this.spectators = new HashSet<>();

        this.procedure = new Procedure(this.main, this);


        if (this.physicalArena) {
            this.spawn = specs.getSpawn();
            this.hologram = new Hologram(this.messageManager.get(Message.GAMEHOLODESC));
            if (specs.getHoloLocation() != null) {
                this.getHologram().setLocation(specs.getHoloLocation());
                this.getHologram().spawn();
            }
        } else {
            this.spawn = null;

        }
    }

    public void announceWinners() {
        int size = this.getPlayingPlayers().size();

        //If there are no playing players left (does not include spectators)
        if (size == 0) {
            if (!broadcastWinners)
                this.sendMessage(this.messageManager.getWithPlaceholder(Message.GAMEMNOWINNERBROADCAST, this.getID()));
            else
                Bukkit.broadcastMessage(Utils.addColour(this.messageManager.getWithPlaceholder(Message.GAMEMNOWINNERBROADCAST, this.getID())));
            return;
        }

        //If there is 1 player remaining (does not include spectators)
        if (size == 1) {
            Player player = this.getPlayingPlayers().iterator().next();

            if (!broadcastWinners)
                this.sendMessage(messageManager.getWithPlaceholder(Message.GAMEMSINGLEPLAYERBROADCAST, player.getName(), this.getID()));
            else
                Bukkit.broadcastMessage(Utils.addColour(messageManager.getWithPlaceholder(Message.GAMEMSINGLEPLAYERBROADCAST, player.getName(), this.getID())));
            return;
        }

        StringBuilder winners = new StringBuilder();
        int i = 0;

        for (Player player : this.getPlayingPlayers()) {
            if (i == size - 1) {
                winners.append(" and ").append(player.getName());
                break;
            }
            winners.append(player.getName());
            if (i > size - 2) {
                winners.append(", ");
            }
            i++;
        }

        if (!broadcastWinners)
            this.sendMessage(messageManager.getWithPlaceholder(Message.GAMEMULTIPLAYERBROADCAST, winners.toString(), this.getID()));
        else
            Bukkit.broadcastMessage(Utils.addColour(messageManager.getWithPlaceholder(Message.GAMEMULTIPLAYERBROADCAST, winners.toString(), this.getID())));
    }

    public void start() {
        if (this.getHologram() != null) this.getHologram().despawn();

        if (this.main.getGameManager().getScheduler(this.getID()) != null) {
            this.main.getGameManager().getScheduler(this.getID()).setPlayed();
        }

        this.procedure.start();
    }

    public void reset(boolean kickPlayers, boolean removedMessage) {
        if (kickPlayers) {
            for (Player player : this.getAllPlayers()) {
                if (removedMessage)
                    player.sendMessage(Utils.addColour(this.messageManager.getWithPlaceholder(Message.GAMEPLAYERREMOVEDTARGET, this.getID())));

                if (this.physicalArena) {
                    if (this.isSpectator(player)) {
                        this.removeSpectator(player);
                    }

                    new TeleportTask(player, this.main.getGameManager().getLobbySpawn()).runTaskLater(main, 1);

                    if (this.main.getGameManager().getClearInvSetting()) restoreInventory(player);
                }
            }

            this.allPlayers.clear();
            this.playingPlayers.clear();
        }
        this.forceStart = false;
        this.sendTitle("", "");
        if (this.getHologram() != null) this.getHologram().despawn();

        if (this.countdown != null) {
            try {
                this.countdown.cancel();
            } catch (IllegalStateException ignored) {
            }
        }
        this.countdown = new Countdown(this.main, this, this.specs.getCountdownSeconds());

        if (this.main.getGameManager().getScheduler(this.getID()) != null) {
            this.main.getGameManager().newScheduler(this.getID(), this.specs.getSchedulerDelay(), this.specs.getSchedulerResetterDelay());
            this.setState(GameState.CLOSED);
        } else this.setState(GameState.RECRUITING);

        this.procedure.onReset();
        this.procedure.unregister();
        this.procedure = new Procedure(this.main, this);

        if (!this.specs.getDescType().equalsIgnoreCase("chat")) {
            if (specs.getHoloLocation() != null) {
                this.getHologram().spawn();
            }
        }
    }

    public int addPlayer(Player player) {
        if (this.state == GameState.CLOSED || this.state == GameState.LIVE) return 2;
        if (this.enforceSurvival) if (player.getGameMode() != GameMode.SURVIVAL) return 1;
        if (this.allPlayers.size() == this.getMaxPlayers()) return 3;

        this.allPlayers.add(player);
        this.playingPlayers.add(player);

        player.sendMessage(Utils.addColour(this.messageManager.getWithPlaceholder(Message.GAMEJOIN, this)));
        this.sendMessage(Utils.addColour(this.messageManager.getWithPlaceholder(Message.GAMEOTHERJOINED, this, player.getName())), player);

        if (this.physicalArena) {
            new TeleportTask(player, this.spawn).runTaskLater(main, 1);
            this.saveInventory(player);
        }

        if (!this.specs.getDescType().equalsIgnoreCase("hologram")) {
            player.sendMessage(Utils.addColour(this.messageManager.get(Message.GAMEDESC)));
        }

        if (this.state == GameState.RECRUITING && this.allPlayers.size() >= this.getMinPlayers()) {
            this.countdown.start();
        } else { //so the sign isnt updated when the player joins AND when the game state changes
            this.updateSigns();
        }
        return 0;
    }

    public void removePlayer(Player player) {
        this.allPlayers.remove(player);
        this.playingPlayers.remove(player);
        player.sendTitle("", "", 0, 20, 0);

        if (this.physicalArena) {
            new TeleportTask(player, this.main.getGameManager().getLobbySpawn()).runTaskLater(main, 1);

            if (this.isSpectator(player)) {
                this.removeSpectator(player);
            }

            this.restoreInventory(player);
        }

        if (this.state != GameState.LIVE)
            this.sendMessage(Utils.addColour(this.messageManager.getWithPlaceholder(Message.GAMEOTHERLEFT, this, player.getName())));


        if (this.allPlayers.size() < this.getMinPlayers()) {
            if (this.state == GameState.COUNTDOWN) {

                if (getForceStart() && !this.allPlayers.isEmpty()) {
                    this.updateSigns();
                    return;
                }

                if (!getForceStart()) {
                    this.sendMessage(this.messageManager.get(Message.GAMENOTENOUGH));
                }

                if (this.countdown != null) {
                    try {
                        this.countdown.cancel();
                    } catch (IllegalStateException ignored) {
                    }
                }
                this.countdown = new Countdown(this.main, this, this.specs.getCountdownSeconds());
                this.setState(GameState.RECRUITING);
                return;
            }
        }
        this.updateSigns();
    }

    public boolean getForceStart() {
        return forceStart;
    }

    public void forceStart() {
        this.forceStart = true;
    }

    public void sendMessage(String message) {
        for (Player player : this.allPlayers) {
            Bukkit.getPlayer(player.getUniqueId()).sendMessage(Utils.addColour(message));
        }
    }

    public void sendMessage(String message, Player excludingPlayer) {
        for (Player player : this.allPlayers) {
            if (player == excludingPlayer) continue;
            Bukkit.getPlayer(player.getUniqueId()).sendMessage(Utils.addColour(message));
        }
    }

    public void sendTitle(String title, String subtitle) {
        for (Player player : this.playingPlayers) {
            Bukkit.getPlayer(player.getUniqueId()).sendTitle(Utils.addColour(title), Utils.addColour(subtitle), 20, 20, 20);
            Bukkit.getPlayer(player.getUniqueId()).sendTitle(Utils.addColour(title), Utils.addColour(subtitle), 20, 20, 20);
        }
    }

    public void end() {
        this.procedure.setCommandEnd();
    }

    public int getID() {
        return specs.getId();
    }

    public GameState getState() {
        return this.state;
    }

    public String getStateDescription() {
        return this.messageManager.get(this.state);
    }

    public void setState(GameState state) {
        this.state = state;

        if (!this.signs.isEmpty()) {
            for (GameSign sign : this.signs) {
                sign.update(this.getStateDescription());
            }
        }
    }

    public String getDescType() {
        return specs.getDescType();
    }

    public boolean isPhysicalArena() {
        return specs.isPhysicalArena();
    }

    public boolean doesBroadcast() {
        return broadcastWinners;
    }

    public HashSet<Player> getAllPlayers() {
        return this.allPlayers;
    }

    public HashSet<Player> getPlayingPlayers() {
        return this.playingPlayers;
    }

    public int getMaxPlayers() {
        return specs.getMaxPlayers();
    }

    public int getMinPlayers() {
        return specs.getMinPlayers();
    }

    public int getMaxRounds() {
        return specs.getMaxRounds();
    }

    public Reward getReward() {
        return specs.getReward();
    }

    public Hologram getHologram() {
        return this.hologram;
    }

    public Countdown getCountdown() {
        return this.countdown;
    }

    public void updateHologramSpawn(Location loc) {
        this.specs.updateHoloLocation(loc);
    }

    public void addSpectator(Player player) {
        if (!this.specManager.setSpectator(player)) {
            this.removePlayer(player);
            player.sendMessage(Utils.addColour(this.messageManager.get(Message.GAMESPECTATORERROR)));
            return;
        }

        this.spectators.add(player);
        this.playingPlayers.remove(player);
        this.updateSigns();
    }

    public void removeSpectator(Player player) {
        this.spectators.remove(player);

        if (player.isOnline()) this.specManager.unSetSpectator(player);
    }

    public boolean isSpectator(Player player) {
        return this.spectators.contains(player);
    }

    public boolean allowsSpectators() {
        return specs.getAllowSpectators();
    }

    public void removePlayingPlayer(Player player) {
        this.playingPlayers.remove(player);
    }

    public void updateSigns() {
        if (!this.signs.isEmpty()) {
            for (GameSign sign : getSigns()) {
                sign.update(this.playingPlayers.size());
            }
        }
    }

    public void addSign(GameSign sign) {
        this.signs.add(sign);
    }

    public void removeSign(String signID) {
        this.getSign(signID).despawn();
        this.signs.remove(this.getSign(signID));
    }

    public ArrayList<GameSign> getSigns() {
        return this.signs;
    }

    public GameSign getSign(String signID) {
        for (GameSign sign : this.signs) {
            if (sign.getID().equals(signID)) {
                return sign;
            }
        }
        return null;
    }

    public void saveInventory(Player player) {
        ItemStack[] inv = player.getInventory().getContents();

        if (this.main.getGameManager().getClearInvSetting()) {
            this.inventories.put(player.getUniqueId(), inv);
//            player.getInventory().clear(); //Broken in 1.21.6 spigot

            for (int i = 0; i < 41; i++) {
                player.getInventory().clear(i);
            }
        }

        if (this.main.getGameManager().getInventoryManager() != null) {
            this.main.getGameManager().getInventoryManager().savePlayer(player, inv);
        }
    }

    public void restoreInventory(Player player) {
        if (!this.inventories.containsKey(player.getUniqueId())) return;

//        player.getInventory().setContents(this.inventories.get(player.getUniqueId()));  //Broken in 1.21.6 spigot

        ItemStack[] savedInv = inventories.get(player.getUniqueId());
        for (int i = 0; i < 41; i++) {
            player.getInventory().setItem(i, savedInv[i]);
        }

        this.inventories.remove(player.getUniqueId());

        if (this.main.getGameManager().getInventoryManager() != null)
            this.main.getGameManager().getInventoryManager().removePlayer(player);
    }

    public void setSpawnLocation(Location location) {
        this.spawn = location;
        this.specs.updateSpawn(location);
    }

    public Location getSpawn() {
        return this.spawn;
    }

    public String getSpawnString() {
        return "World: " + spawn.getWorld().getName() + " X=" + Math.round(spawn.getX()) + ", Y=" + Math.round(spawn.getY()) + ", Z=" + Math.round(spawn.getZ());
    }
}