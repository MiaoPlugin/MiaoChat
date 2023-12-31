package pw.yumc.MiaoChat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import pw.yumc.MiaoChat.MiaoChat;
import pw.yumc.MiaoChat.MiaoMessage;
import pw.yumc.MiaoChat.config.ChatConfig;
import pw.yumc.MiaoChat.config.ChatRule;
import pw.yumc.YumCore.bukkit.Log;
import pw.yumc.YumCore.bukkit.P;
import pw.yumc.YumCore.global.L10N;
import pw.yumc.YumCore.statistic.Statistics;
import pw.yumc.YumCore.tellraw.Tellraw;
import pw.yumc.YumCore.update.SubscribeTask;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author MiaoWoo
 */
public class ChatListener implements Listener {
    public static Set<Player> offList = new HashSet<>();
    private static Pattern PATTERN = Pattern.compile("%([a-z1-9]?)");

    private MiaoChat plugin = P.getPlugin();
    private ChatConfig cc = plugin.getChatConfig();

    public ChatListener() {
        Bukkit.getPluginManager().registerEvents(this, P.instance);
        new Statistics();
        new SubscribeTask(true, SubscribeTask.UpdateType.MAVEN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        ChatRule cr = cc.getChatRule(e.getPlayer());
        if (cr == null) {
            // Log.d("玩家: %s 未发现可用ChatRule!", p.getName());
            return;
        }
        handleChat(p, e.getRecipients(), cr, e.getMessage());
    }

    private void handleChat(Player p, Set<Player> receive, ChatRule cr, String message) {
        // Log.d("玩家: %s 使用 %s 规则 解析 %s", p.getName(), cr.getName(), message);
        handleSend(p, receive, handleTellraw(p, cr.create(p), cr, message), cr.getRange());
    }

    private LinkedList<String> handleMessage(LinkedList<String> il, String message) {
        LinkedList<String> mlist = new LinkedList<>();
        // Log.d("处理聊天信息...");
        if (!il.isEmpty()) {
            for (String k : il) {
                String[] args = message.split(k, 2);
                if (!args[0].isEmpty()) {
                    // Log.d("追加文本: %s", args[0]);
                    mlist.add(args[0]);
                }
                // Log.d("解析物品: %s", args[0]);
                mlist.add(k);
                message = args[1];
            }
        }
        if (!message.isEmpty()) {
            // Log.d("追加文本: %s", message);
            mlist.add(message);
        }
        return mlist;
    }

    private LinkedList<String> handlePattern(String message) {
        Matcher m = PATTERN.matcher(message);
        Set<String> temp = new HashSet<>();
        LinkedList<String> ilist = new LinkedList<>();
        // Log.d("处理聊天物品信息...");
        while (m.find()) {
            String key = m.group(0);
            if (key.length() == 2) {
                if (temp.add(key)) {
                    // Log.d("解析物品关键词: %s", key);
                    ilist.add(key);
                } else {
                    return null;
                }
            }
        }
        return ilist;
    }

    private void handleSend(Player p, Set<Player> receive, Tellraw tr, int range) {
        Set<Player> plist = new HashSet<>();
        if (range != 0) {
            p.getNearbyEntities(range, range, range).stream().filter(entity -> entity instanceof Player).forEach(entity -> plist.add((Player) entity));
            plist.add(p);
        } else {
            plist.addAll(receive);
            if (cc.isBungeeCord()) {
                byte[] mm = MiaoMessage.encode(tr.toJsonString());
                // 数据流等于NULL代表数据超长
                if (mm == null) {
                    p.sendPluginMessage(P.instance, MiaoMessage.NORMAL_CHANNEL, MiaoMessage.encode(tr.toOldMessageFormat()));
                } else {
                    p.sendPluginMessage(P.instance, MiaoMessage.CHANNEL, mm);
                }
            }
        }
        receive.clear();
        plist.removeAll(offList);
        plist.forEach(tr::send);
    }

    private Tellraw handleTellraw(Player player, Tellraw tr, ChatRule cr, String message) {
        if (message.isEmpty()) {return tr;}
        if (player.hasPermission("MiaoChat.color")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }
        if (player.hasPermission("MiaoChat.rgb")) {
            message = MiaoMessage.rgb(message);
        }
        if (!cr.isItem()) {
            tr.then(message);
            return tr;
        }
        LinkedList<String> il = handlePattern(message);
        // 如果返回null说明存在相同的物品
        if (il == null) {
            Log.sender(player, "§c不允许展示相同的物品!");
            return tr;
        }
        LinkedList<String> ml = handleMessage(il, message);
        // Log.d("处理Tellraw格式...");
        while (!ml.isEmpty()) {
            String mm = ml.removeFirst();
            if (il.contains(mm)) {
                char k = mm.charAt(1);
                String key = String.valueOf(k);
                if (plugin.getChatConfig().getFormats().containsKey(key)) {
                    if (!player.hasPermission("MiaoChat.format.*") && player.hasPermission("MiaoChat.format." + k)) {
                        Log.sender(player, "§c你没有使用 " + mm + " 的权限!");
                        continue;
                    }
                    plugin.getChatConfig().getFormats().get(key).then(tr, player);
                } else {
                    ItemStack is = null;
                    if (k == 'i') {
                        is = player.getItemInHand();
                    } else {
                        int index = k - '0' - 1;
                        if (index < 10) {
                            is = player.getInventory().getItem(index);
                        }
                    }
                    if (is != null && is.getType() != Material.AIR) {
                        // Log.d("处理物品: %s", mm);
                        tr.then(String.format(ChatColor.translateAlternateColorCodes('&', cr.getItemformat()), L10N.getName(is)));
                        tr.item(is);
                    } else {
                        tr.then(cr.getLastColor() + mm);
                    }
                }
            } else {
                // Log.d("追加聊天: %s", mm);
                tr.then(cr.getLastColor() + mm);
            }
        }
        return tr;
    }
}
