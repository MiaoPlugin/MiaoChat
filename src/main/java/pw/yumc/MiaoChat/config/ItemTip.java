package pw.yumc.MiaoChat.config;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pw.yumc.MiaoChat.bungee.Log;
import pw.yumc.YumCore.config.annotation.Default;
import pw.yumc.YumCore.config.annotation.Nullable;
import pw.yumc.YumCore.config.inject.InjectConfigurationSection;

import java.util.List;

public class ItemTip extends InjectConfigurationSection {
    private String type;
    @Default("0")
    private Short damage;
    @Nullable
    private String name;

    private transient ItemStack itemStack;
    private transient ItemMeta itemMeta;

    public ItemTip(ConfigurationSection config) {
        super(config);
    }

    @Override
    protected void init() {
        super.init();
        try {
            Material material = Material.valueOf(type);
            this.itemStack = new ItemStack(material, 1, damage);
            this.itemMeta = Bukkit.getItemFactory().getItemMeta(material);
        } catch (Throwable ex) {
            this.itemStack = new ItemStack(Material.STONE, 1);
            Log.w("物品 %s 解析失败 将使用默认值 STONE...", type);
        }
    }

    public ItemStack getItemStack(Player p, String name, List<String> tip) {
        ItemStack itemStack = this.itemStack.clone();
        ItemMeta itemMeta = this.itemMeta.clone();
        itemMeta.setDisplayName(PlaceholderAPI.setPlaceholders(p, this.name == null ? name : this.name));
        itemMeta.setLore(PlaceholderAPI.setPlaceholders(p, tip));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
