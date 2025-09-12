package me.soapiee.common.command;

import me.soapiee.common.TFQuiz;
import me.soapiee.common.enums.Message;
import me.soapiee.common.instance.Game;
import me.soapiee.common.manager.MessageManager;
import me.soapiee.common.utils.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerCommand implements CommandExecutor, TabCompleter {

    private final TFQuiz main;
    private final MessageManager messageManager;

    public PlayerCommand(TFQuiz main) {
        this.main = main;
        this.messageManager = main.getMessageManager();
    }

    @Override     // /game <join | leave | list> <gameID>
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            Utils.consoleMsg("You must be a player to use this command");
            return true;
        }

        Player player = (Player) sender;
        String cmdUsage = Utils.colour(this.messageManager.get(Message.GAMECCMDUSAGE));
        String noPermission = Utils.colour(this.messageManager.get(Message.NOPERMISSION));

        if (args.length == 0) {
            player.sendMessage(cmdUsage);
            return true;
        }

        if (!player.hasPermission("tfquiz.player")) {
            player.sendMessage(noPermission);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "join":
                if (!player.hasPermission("tfquiz.player.join")) {
                    player.sendMessage(noPermission);
                    return true;
                }

                if (args.length != 2) {
                    player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMEJOINCMDUSAGE)));
                    return true;
                }

                if (this.main.getGameManager().getGame(player) != null) {
                    player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMENOTNULL)));
                    return true;
                }

                //Check they provided a number/int
                String invalidGameID = Utils.colour(this.messageManager.get(Message.GAMEINVALIDGAMEID));
                int gameID;
                try {
                    gameID = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(invalidGameID);
                    return true;
                }

                //Check the game is a valid game
                if (this.main.getGameManager().getGame(gameID) != null) {
                    Game gameToJoin = this.main.getGameManager().getGame(gameID);
                    switch (gameToJoin.addPlayer(player)) {
                        case 1:
                            player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMEINVALIDGAMEMODE)));
                            return true;
                        case 2:
                            player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMEINVALIDSTATE)));
                            return true;
                        case 3:
                            player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMEFULL)));
                            return true;
                        case 0:
                            return true;
                    }
                } else {
                    player.sendMessage(invalidGameID);
                }
                return true;

            case "leave":
                if (!player.hasPermission("tfquiz.player.join")) {
                    player.sendMessage(noPermission);
                    return true;
                }

                if (args.length != 1) break;
                Game gameToLeave = this.main.getGameManager().getGame(player);
                if (gameToLeave != null) {
                    gameToLeave.removePlayer(player);
                    player.sendMessage(Utils.colour(this.messageManager.getWithPlaceholder(Message.GAMELEAVE, gameToLeave)));
                } else {
                    player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMELEFTERROR)));
                }
                return true;

            case "list":
                if (!player.hasPermission("tfquiz.player.list")) {
                    player.sendMessage(noPermission);
                    return true;
                }
                if (args.length != 1) break;
                player.sendMessage(Utils.colour(this.messageManager.get(Message.GAMELISTHEADER)));
                for (Game game : this.main.getGameManager().getGames()) {
                    String message = this.messageManager.getWithPlaceholder(Message.GAMELIST, game);
                    TextComponent clickableText = new TextComponent(Utils.colour(message));
                    clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/game join " + game.getID()));
                    clickableText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Utils.colour(this.messageManager.get(Message.GAMELISTHOVER)))));
                    player.spigot().sendMessage(clickableText);
                }
                return true;

            default:
                player.sendMessage(cmdUsage);
                return true;
        }
        player.sendMessage(cmdUsage);
        return true;
    }

    @Override      //      Usage: /game <join|leave|list> <ID>
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        final List<String> results = new ArrayList<>();
        switch (args.length) {
            case 1:
                if (sender instanceof Player && sender.hasPermission("tfquiz.player.list")) {
                    results.add("list");
                }
                if (sender instanceof Player && sender.hasPermission("tfquiz.player.join")) {
                    results.add("join");
                    results.add("leave");
                }
                break;
            case 2:
                if (sender instanceof Player && !sender.hasPermission("tfquiz.player.join")) break;
                if (args[0].equals("join")) {
                    for (Game game : this.main.getGameManager().getGames()) {
                        results.add(String.valueOf(game.getID()));
                    }
                }
                break;
        }
        return results.stream().filter(completion -> completion.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }
}
