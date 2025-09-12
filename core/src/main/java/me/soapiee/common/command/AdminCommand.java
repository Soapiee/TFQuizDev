package me.soapiee.common.command;

import me.soapiee.common.TFQuiz;
import me.soapiee.common.enums.GameState;
import me.soapiee.common.enums.Message;
import me.soapiee.common.instance.Game;
import me.soapiee.common.instance.cosmetic.GameSign;
import me.soapiee.common.instance.logic.TeleportTask;
import me.soapiee.common.listener.PlayerListener;
import me.soapiee.common.manager.GameManager;
import me.soapiee.common.manager.MessageManager;
import me.soapiee.common.utils.Keys;
import me.soapiee.common.utils.Logger;
import me.soapiee.common.utils.PlayerCache;
import me.soapiee.common.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final TFQuiz main;
    private final PlayerCache playerCache;
    private final PlayerListener playerListener;
    private final MessageManager messageManager;
    private final GameManager gameManager;
    private final Logger logger;

    private final HashSet<UUID> confirmation;

    public AdminCommand(TFQuiz main, PlayerListener playerListener) {
        this.main = main;
        playerCache = main.getPlayerCache();
        this.playerListener = playerListener;
        messageManager = main.getMessageManager();
        gameManager = main.getGameManager();
        logger = main.getCustomLogger();
        confirmation = new HashSet<>();
    }

    public void reloadCheck(CommandSender sender) {
        sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINRELOADINPROGRESS)));
        String reloadOutcome = Utils.colour(messageManager.get(Message.ADMINRELOADSUCCESS));

        for (Game game : gameManager.getGames()) {
            game.reset(true, true);
            if (game.getHologram() != null) game.getHologram().despawn();
            game.setState(GameState.CLOSED);
        }

        boolean errors = false;
        if (!messageManager.load(sender)) errors = true;
        if (!gameManager.reloadAll(sender, playerListener)) errors = true;

        if (errors) reloadOutcome = Utils.colour(messageManager.get(Message.ADMINRELOADERROR));

        if (sender instanceof Player) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + sender.getName() + " " + reloadOutcome);
        }

        sender.sendMessage(reloadOutcome);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof CommandBlock) return true;


        String adminHelp = Utils.colour(messageManager.get(Message.ADMINCMDUSAGE));
        if (args.length == 0) {
            sender.sendMessage(adminHelp);
            return true;
        }

        if (sender instanceof Player && !sender.hasPermission("tfquiz.admin.*")) {
            sender.sendMessage(Utils.colour(messageManager.get(Message.NOPERMISSION)));
            return true;
        }

        Player player = null;
        if (sender instanceof Player) player = (Player) sender;

        String argument = args[0].toLowerCase();
        switch (argument) {
            case "reload":
                if (sender instanceof Player && !sender.hasPermission("TFQuiz.reload")) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.NOPERMISSION)));
                    return true;
                }
                if (args.length == 1) {
                    if (player != null) { // sender is instance of a player
                        confirmation.add(player.getUniqueId());
                    }
                    sender.sendMessage(Utils.colour("&cThis will force reset all games, all scheduled games, and return all players to the lobby spawn"));
                    sender.sendMessage(Utils.colour("&cType '/tf reload confirm' to confirm"));
                    return true;
                }
                if (args.length == 2) {
                    if (player != null) { // sender is instance of a player
                        if (args[1].equals("confirm")) {
                            if (confirmation.contains(player.getUniqueId())) {
                                reloadCheck(player);
                                confirmation.remove(player.getUniqueId());
                                return true;
                            }
                            sender.sendMessage(adminHelp);
                            return true;
                        }
                    } else { //is console
                        reloadCheck(sender);
                        return true;
                    }
                }
                sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINRELOADCMDUSAGE)));
                return true;

            case "setspawn":
                if (player == null) { // sender is console
                    sender.sendMessage(Utils.colour(messageManager.get(Message.CONSOLEUSAGEERROR)));
                    return true;
                }
                if (args.length != 1) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINSETLOBBYSPAWNCMDUSAGE)));
                    return true;
                }
                gameManager.setLobbySpawn(player.getLocation());
                sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINSETLOBBYSPAWN)));
                return true;
            case "list":
                if (args.length != 1) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINLISTCMDUSAGE)));
                    return true;
                }

                sender.sendMessage(Utils.colour(messageManager.get(Message.GAMELISTHEADER)));
                for (Game game : gameManager.getGames()) {
                    String list = messageManager.getWithPlaceholder(Message.GAMELIST, game);
                    sender.sendMessage(Utils.colour(list));
                }
                return true;
            case "removeholos":
                int count = 0;

                if (args.length == 1) {
                    Location loc = player.getLocation();
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, loc.getX() + 3, loc.getY() + 3, loc.getX() + 3)) {
                        if (entity instanceof ArmorStand) {
                            entity.remove();
                            count++;
                        }
                    }
                    sender.sendMessage(Utils.colour("&a" + count + " holograms were removed near you"));
                    return true;
                }

                if (args.length == 2 && args[1].equalsIgnoreCase("-all")) {
                    for (Entity entity : Bukkit.getWorld("world").getEntities()) {
                        if (entity instanceof ArmorStand && entity.getPersistentDataContainer().has(Keys.HOLOGRAM_ARMOURSTAND, PersistentDataType.BYTE)) {
                            entity.remove();
                            count++;
                        }
                    }
                    sender.sendMessage(Utils.colour("&a" + count + " holograms were removed"));
                    return true;
                }

                sender.sendMessage(Utils.colour(adminHelp));
                return true;

//            case "addgame":
//                if (args.length != 2) {
//                    sender.sendMessage(Utils.colour(messageManager.get(Message.ADMINLISTCMDUSAGE)));
//                    return true;
//                }
//
//                gameManager.addGame();
////                sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEADDED)));
////                sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEADDEDERROR)));
//                return true;

            case "game":
                String gameHelp = messageManager.get(Message.GAMEADMINCMDUSAGE);
                if (args.length < 3 || args.length > 5) {
                    sender.sendMessage(Utils.colour(gameHelp));
                    return true;
                }

                int gameID;
                Game game;
                try {
                    gameID = Integer.parseInt(args[1]);
                    game = gameManager.getGame(gameID);
                } catch (NumberFormatException | NullPointerException ex) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEINVALIDGAMEID)));
                    return true;
                }
                if (game == null) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEINVALIDGAMEID)));
                    return true;
                }

                switch (args[2].toLowerCase()) {
//                    case "delete":
//                        if (args.length != 3) {
//                            sender.sendMessage(adminHelp);
//                            return true;
//                        }
//
//                        gameManager.deleteGame(game);
////                sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEDELETED)));
//                        return true;
                    case "setspawn": // /tf game <gameID> setspawn
                        if (args.length != 3) {
                            sender.sendMessage(adminHelp);
                            return true;
                        }
                        if (player == null) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.CONSOLEUSAGEERROR)));
                            return true;
                        }
                        Player player1 = (Player) sender;
                        game.setSpawnLocation(player1.getLocation());
                        sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMESPAWNSET, game.getID())));
                        return true;
                    case "setholospawn":
                        if (player == null) { // sender is console
                            sender.sendMessage(Utils.colour(messageManager.get(Message.CONSOLEUSAGEERROR)));
                            return true;
                        }
                        if (args.length != 3) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        if (game.getHologram() != null) {
                            game.getHologram().despawn();
                            game.getHologram().setLocation(player.getLocation());
                        }
                        game.updateHologramSpawn(player.getLocation());
                        if (game.getState() != GameState.LIVE && game.getDescType().equals("hologram") || game.getDescType().equals("both"))
                            game.getHologram().spawn();
                        sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEHOLOSPAWNSET, game.getID())));
                        return true;
                    case "open":
                        if (args.length != 3) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        //Check if it has a scheduler
                        if (gameManager.getScheduler(game.getID()) != null) {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEOPENEDERROR2, game)));
                            return true;
                        }

                        if (game.getState() == GameState.CLOSED) {
                            game.setState(GameState.RECRUITING);
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEOPENED, game)));
                        } else {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEOPENEDERROR, game)));
                        }
                        return true;
                    case "close":
                        if (args.length != 3) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        //Check if it has a scheduler
                        if (gameManager.getScheduler(game.getID()) != null) {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMECLOSEDERROR2, game)));
                            return true;
                        }

                        if (game.getState() == GameState.RECRUITING || game.getState() == GameState.COUNTDOWN) {
                            game.reset(true, true);
                            game.setState(GameState.CLOSED);
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMECLOSED, game)));
                        } else {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMECLOSEDERROR, game)));
                        }
                        return true;
                    case "start": // /tf game <gameID> start
                        if (args.length != 3) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        //Check if it has a scheduler
                        if (gameManager.getScheduler(game.getID()) != null) {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMESTARTSCHEDULERERROR, game)));
                            return true;
                        }


                        if (game.getState() == GameState.LIVE || game.getState() == GameState.COUNTDOWN) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEFORCESTARTERROR)));
                            return true;
                        }

                        if (game.getState() == GameState.CLOSED) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.GAMESTARTCLOSEDERROR)));
                            return true;
                        }

                        //Check there is at least 1 player
                        if (game.getAllPlayers().isEmpty()) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.GAMESTARTEMPTYERROR)));
                            return true;
                        }

                        game.forceStart();
                        game.getCountdown().start();
                        sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEFORCESTARTED, game)));
                        return true;
                    case "end": // /tf game <gameID> end -without
                        if (args.length == 5) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        //Check if it has a scheduler
                        if (gameManager.getScheduler(game.getID()) != null) {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEENDSCHEDULERERROR, game)));
                            return true;
                        }

                        if (game.getState() == GameState.CLOSED || game.getState() == GameState.RECRUITING) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEFORCEENDERROR)));
                            return true;
                        }

                        if (game.getState() == GameState.COUNTDOWN) {
                            game.reset(false, false);
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEFORCEENDED, gameID)));
                            return true;
                        }

                        if (game.getState() == GameState.LIVE) {
                            if (args.length == 4 && args[3].equals("-without")) {
                                game.reset(true, true); //without winners
                                sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEFORCEENDED, gameID)));
                            } else { // with winners
                                game.end();
                                sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEFORCEENDEDWITHWINNERS, gameID)));
                            }
                        }
                        return true;
                    case "addplayer": // /tf game <gameID> addplayer <playerName>
                        if (args.length != 4) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        if (game.getState() == GameState.LIVE || game.getState() == GameState.CLOSED) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEPLAYERADDEDERROR)));
                        } else {
                            Player target = Bukkit.getPlayer(args[3]);
                            if (target == null) {
                                sender.sendMessage(Utils.colour(messageManager.get(Message.PLAYERNOTFOUND)));
                                return true;
                            }
                            if (gameManager.getGame(target) != null) {
                                sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEPLAYERALREADYINGAME, target.getName())));
                                return true;
                            }
                            int outcome = game.addPlayer(target);
                            switch (outcome) {
                                case 1:
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEINVALIDGAMEMODEOTHER)));
                                    return true;
                                case 2:
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEINVALIDSTATE)));
                                    return true;
                                case 3:
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEFULL)));
                                    return true;
                                case 0:
                                    sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEPLAYERADDED, target.getName(), gameID)));
                                    return true;
                            }
                        }
                        return true;
                    case "removeplayer": // /tf game <gameID> removeplayer <playerName>
                        if (args.length != 4) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        Player target = Bukkit.getPlayer(args[3]);
                        if (target == null) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.PLAYERNOTFOUND)));
                            return true;
                        }

                        Game targetsGame = gameManager.getGame(target);
                        if (targetsGame == null || targetsGame.getID() != gameID) {
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEPLAYERNOTINGAME, target.getName())));
                            return true;
                        } else {
                            target.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.FORCEDGAMELEAVE, gameID)));
                            game.removePlayer(target);
                            sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.GAMEPLAYERREMOVED, target.getName(), gameID)));
                        }
                        return true;
                    case "info": // /tf game <gameID> info
                        if (args.length != 3) {
                            sender.sendMessage(Utils.colour(gameHelp));
                            return true;
                        }

                        sender.sendMessage(Utils.colour(messageManager.getInfo(Message.GAMEINFO, game)));
                        return true;
                    default:
                        sender.sendMessage(Utils.colour(gameHelp));
                        return true;
                }
            case "sign":
                // /tf sign remove|edit (Looking at sign)
                // /tf sign add|remove|edit <ID> ...
                if (!sender.hasPermission("tfquiz.admin.signs")) {
                    sender.sendMessage(Utils.colour(messageManager.get(Message.NOPERMISSION)));
                    return true;
                }

                String signHelp = Utils.colour(messageManager.get(Message.SIGNADMINCMDUSAGE));

                if (args.length < 2) {
                    sender.sendMessage(signHelp);
                    return true;
                }
                if (player == null) { //is console
                    if (args[1].equals("add")) {
                        sender.sendMessage(Utils.colour(messageManager.get(Message.CONSOLEUSAGEERROR)));
                        return true;
                    }
                    if (!args[1].equals("list") && args.length < 3) {
                        sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNINVALIDSIGNID)));
                        return true;
                    }
                }

                String notLookingAtGameSign = Utils.colour(messageManager.get(Message.SIGNNOTLOOKINGATGAMESIGN));

                switch (args[1].toLowerCase()) {
                    case "list":
                        if (args.length != 2) {
                            sender.sendMessage(signHelp);
                            return true;
                        }

                        sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNLISTHEADER)));
                        for (Game games : gameManager.getGames()) {
                            for (GameSign signs : games.getSigns()) {
                                String message = messageManager.getWithPlaceholder(Message.SIGNLISTFORMAT, signs.getID(), games.getID());
                                if (player != null) {
                                    TextComponent clickableText = new TextComponent(Utils.colour(message));
                                    clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tf sign teleport " + signs.getID()));
                                    clickableText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Utils.colour(messageManager.get(Message.SIGNLISTHOVER)))));
                                    player.spigot().sendMessage(clickableText);
                                } else {
                                    sender.sendMessage(Utils.colour(message));
                                }
                            }
                        }
                        return true;

                    case "teleport": // /tf sign teleport <signID>
                        if (player == null) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.CONSOLEUSAGEERROR)));
                            return true;
                        }

                        GameSign sign = gameManager.getSign(args[2]);
                        new TeleportTask(player, sign.getLocation()).runTaskLater(main, 1);
                        return true;

                    case "edit":
                        // /tf sign edit <lineNo> "text..."
                        // /tf sign edit <ID> <lineNo> "text..."
                        int lineNo = -1;
                        String signID = null;
                        Sign block = null;

                        if (args.length < 4) {
                            sender.sendMessage(signHelp);
                            return true;
                        }

                        if (player != null) { // sender is instance of Player
                            Block targetBlock = player.getTargetBlock(null, 5);

                            if (targetBlock.getState() instanceof Sign) {
                                block = (Sign) targetBlock.getState();
                                signID = block.getPersistentDataContainer().get(Keys.GAME_SIGN, PersistentDataType.STRING);
                            }
                        }

                        if (block == null) { // is console or not looking at a sign
                            try {
                                Integer.parseInt(args[2]);
                                lineNo = Integer.parseInt(args[3]) - 1;
                            } catch (NumberFormatException ex) {
                                sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNEDITIDCMDUSAGE)));
                                return true;
                            }
                            signID = args[2];
                        } else {
                            try {
                                lineNo = Integer.parseInt(args[2]) - 1;
                            } catch (NumberFormatException ex) {
                                sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNEDITCMDUSAGE)));
                                return true;
                            }

                        }
                        if (lineNo < 0 || lineNo > 3) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNINVALIDLINENUM)));
                            return true;
                        }

                        GameSign gameSign = gameManager.getSign(signID);
                        if (gameSign == null) {
                            sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNINVALIDSIGNID)));
                            return true;
                        }

                        StringBuilder builder = new StringBuilder();
                        int a = 4;
                        if (block != null) a = 3;
                        for (int i = a; i <= args.length - 1; i++) {
                            builder.append(args[i]);
                            if (i != args.length - 1) builder.append(" ");
                        }

                        gameSign.update(lineNo, builder.toString());
                        gameManager.saveSign(gameSign);

                        sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.SIGNEDITED, signID)));
                        return true;

                    case "add":
                    case "remove":
                        // /tf sign add <gameID> (looking at sign)
                        // /tf sign remove (Looking at sign)
                        // /tf sign remove <signID>
                        if (args.length > 3) {
                            sender.sendMessage(signHelp);
                            return true;
                        }
                        String invalidGameID = Utils.colour(messageManager.get(Message.GAMEINVALIDGAMEID));
                        if (args.length == 2 && args[1].equals("add")) {
                            sender.sendMessage(invalidGameID);
                            return true;
                        }


                        String inputID = null;
                        if (args.length == 3) { // a signID was provided
                            try {
                                Integer.parseInt(args[2]);
                            } catch (NumberFormatException ex) {
                                if (args[1].equals("add")) sender.sendMessage(invalidGameID);
                                else
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNINVALIDSIGNID)));
                                return true;
                            }
                            inputID = args[2];
                        }

                        Sign signBlock = null;
                        String dataContainer = null;
                        if (player != null) { // sender is instance of Player
                            Block blockTarget = player.getTargetBlock(null, 5);

                            if (blockTarget.getState() instanceof Sign) {
                                signBlock = (Sign) blockTarget.getState();
                                dataContainer = signBlock.getPersistentDataContainer().get(Keys.GAME_SIGN, PersistentDataType.STRING);

                                if (args[1].equals("remove")) {
                                    if (dataContainer == null) {
                                        sender.sendMessage(notLookingAtGameSign);
                                        return true;
                                    } else
                                        inputID = dataContainer;
                                }
                            }
                        }


                        if (signBlock == null && player != null) { // will be null when not looking at a sign
                            if (inputID == null) {
                                player.sendMessage(notLookingAtGameSign);
                                return true;
                            }
                            if (args[1].equals("add")) {
                                player.sendMessage(Utils.colour(messageManager.get(Message.SIGNNOTLOOKINGATSIGN)));
                                return true;
                            }
                        }

                        if (inputID == null) { //should not be reachable
                            logger.logToPlayer(sender, null, "There is an error in add/remove sign command. Please report this to the developer");
                            return true;
                        }

                        switch (args[1].toLowerCase()) {
                            case "add":
                                //Checks the sign isn't already a game sign
                                if (dataContainer != null && gameManager.getSign(dataContainer) != null) {
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNALREADYEXISTS)));
                                    return true;
                                }

                                //Checks the gameID relates to a configured game
                                if (gameManager.getGame(Integer.parseInt(inputID)) == null) {
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.GAMEINVALIDGAMEID)));
                                    return true;
                                }

                                gameManager.saveSign(signBlock, Integer.parseInt(inputID));
                                player.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.SIGNADDED, inputID)));
                                return true;

                            case "remove":
                                //Checks the signID relates to a configured game sign
                                if (gameManager.getSign(inputID) == null) {
                                    sender.sendMessage(Utils.colour(messageManager.get(Message.SIGNINVALIDSIGNID)));
                                    return true;
                                }

                                GameSign signToRemove = gameManager.getSign(inputID);
                                signToRemove.getLocation().getWorld().dropItem(signToRemove.getLocation(), new ItemStack(signToRemove.getMaterial()));
                                gameManager.deleteSign(inputID);
                                sender.sendMessage(Utils.colour(messageManager.getWithPlaceholder(Message.SIGNREMOVED, inputID)));
                                return true;
                        }
                    default:
                        sender.sendMessage(signHelp);
                        return true;
                }
            default:
                sender.sendMessage(adminHelp);
                return true;
        }
    }

    @Override
//      Usage: /tf <list|setspawn|game|sign> <game ID> <open|close|add|remove|setspawn> <locID>
//      Usage: /tf <list|setspawn|sign> <sign ID> <list|add|remove|edit> <lineNo> text...
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        final List<String> results = new ArrayList<>();

        switch (args.length) {
            case 1:
                if (sender instanceof Player && !sender.hasPermission("tfquiz.admin.*")) break;
                results.add("help");
                results.add("list");
                results.add("setspawn");
                results.add("game");
                if (sender instanceof Player && sender.hasPermission("tfquiz.admin.signs")) results.add("sign");
                if (sender instanceof Player && sender.hasPermission("tfquiz.reload")) results.add("reload");
                break;
            case 2:
                if (args[0].equalsIgnoreCase("sign") && sender.hasPermission("tfquiz.admin.signs")) {
                    results.add("add");
                    results.add("remove");
                    results.add("edit");
                    results.add("list");
                }
                if (args[0].equalsIgnoreCase("game") && sender.hasPermission("tfquiz.admin.*")) {
                    for (Game game : gameManager.getGames()) {
                        results.add(String.valueOf(game.getID()));
                    }
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("game") && sender.hasPermission("tfquiz.admin.*")) {
                    results.add("setspawn");
                    results.add("setholospawn");
                    results.add("open");
                    results.add("close");
                    results.add("start");
                    results.add("end");
                    results.add("addPlayer");
                    results.add("removePlayer");
                    results.add("info");
                }
                break;
            case 4:
                if (args[2].equalsIgnoreCase("addplayer") || args[2].equalsIgnoreCase("removeplayer")) {
                    for (final OfflinePlayer player : playerCache.getList()) {
                        results.add(player.getName().toLowerCase());
                    }
                } else if (args[2].equalsIgnoreCase("end")) results.add("-without");
                break;
        }
        return results.stream().filter(completion -> completion.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }
}
