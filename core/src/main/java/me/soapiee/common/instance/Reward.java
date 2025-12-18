package me.soapiee.common.instance;

import me.soapiee.common.TFQuiz;
import me.soapiee.common.enums.Message;
import me.soapiee.common.enums.RewardType;
import me.soapiee.common.hooks.VaultHook;
import me.soapiee.common.manager.MessageManager;
import me.soapiee.common.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Reward {

    private final MessageManager messageManager;
    private VaultHook vaultHook;
    private final RewardType type;
    private final String message;
    private ArrayList<ItemStack> itemList;
    private ArrayList<String> permissionList;
    private ArrayList<String> commandsList;
    private int xpAmount;
    private double money;

    public Reward(TFQuiz main, RewardType type, String message) {
        this.messageManager = main.getMessageManager();
        this.type = type;
        this.message = message;
    }

    //Currency Type
    public Reward(TFQuiz main, RewardType type, String message, double amount) {
        this(main, type, message);
        this.vaultHook = main.getVaultHook();
        this.money = amount;
    }

    //XP Type
    public Reward(TFQuiz main, RewardType type, String message, int amount) {
        this(main, type, message);
        this.xpAmount = amount;
    }

    //Item Type
    public Reward(TFQuiz main, RewardType type, String message, List<ItemStack> list) {
        this(main, type, message);
        this.itemList = (ArrayList<ItemStack>) list;
    }

    //Command + Permission Type
    public Reward(TFQuiz main, RewardType type, String message, ArrayList<String> list) {
        this(main, type, message);
        switch (type) {
            case PERMISSION:
                this.vaultHook = main.getVaultHook();
                this.permissionList = list;
                break;
            case COMMAND:
                this.commandsList = list;
                break;
        }
    }

    public void give(Player player) {
        switch (type) {
            case PERMISSION:
                for (String permission : getPermissionList()) {
                    vaultHook.setPermission(player, permission);
                }
                break;
            case CURRENCY:
                vaultHook.deposit(player, getMoneyAmount());
                break;
            case EXPERIENCE:
                player.giveExp(getxpAmount());
                break;
            case ITEM:
                for (ItemStack item : getItemList()) {
                    if (Utils.hasFreeSpace(item.getType(), item.getAmount(), player)) {
                        player.getInventory().addItem(item);
                    } else {
                        player.getLocation().getWorld().dropItem(player.getLocation(), item);
                        player.sendMessage(Utils.addColour(messageManager.get(Message.GAMEITEMWINERROR)));
                        return;
                    }
                }
                break;
            case COMMAND:
                for (String command : getCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                }
                break;
            case NONE:
                break;
        }
        if (getMessage() != null) player.sendMessage(Utils.addColour(getMessage()));
    }

    public RewardType getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }

    public ArrayList<ItemStack> getItemList() {
        return this.itemList;
    }

    public ArrayList<String> getCommands() {
        return this.commandsList;
    }

    public int getxpAmount() {
        return this.xpAmount;
    }

    public ArrayList<String> getPermissionList() {
        return this.permissionList;
    }

    public double getMoneyAmount() {
        return this.money;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        int i = 1;

        switch (type) {
            case COMMAND:
                builder.append(type.toString().toLowerCase()).append("s: ");
                for (String permission : commandsList) {
                    builder.append(permission);
                    if (commandsList.size() > i) builder.append(", ");
                    i++;
                }
                break;
            case PERMISSION:
                builder.append(type.toString().toLowerCase()).append("s: ");
                for (String permission : permissionList) {
                    builder.append(permission);
                    if (permissionList.size() > i) builder.append(", ");
                    i++;
                }
                break;
            case ITEM:
                for (ItemStack item : itemList) {
                    builder.append(item.getAmount()).append(" ").append(item.getType().toString().toLowerCase().replace("_", " "));
                    if (itemList.size() > i) builder.append(", ");
                    i++;
                }
                break;
            case CURRENCY:
                builder.append(money).append(vaultHook.getCurrencyName());
                break;
            case EXPERIENCE:
                builder.append(xpAmount).append(" exp");
                break;
        }

        return builder.toString();
    }
}
