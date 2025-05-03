// src/main/java/top/nstu/adminGUI/AdminGUI.java
package top.nstu.adminGUI;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.BanList;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AdminGUI extends JavaPlugin implements Listener {

    // 状态存储
    private final Map<UUID, Boolean> trailPlayers = new HashMap<>();
    private final Map<UUID, Boolean> invisiblePlayers = new HashMap<>();
    private final Set<UUID> mutedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("admingui").setExecutor(this);
    }

    // ================= GUI 工具方法 =================
    private ItemStack createToggleItem(Material material, boolean isEnabled, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((isEnabled ? "§a" : "§c") + name + (isEnabled ? " §7(已启用)" : " §7(已关闭)"));
        meta.setLore(Collections.singletonList("§7点击切换状态"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGUIItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    // ================= 主界面 =================
    private void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6管理面板");

        gui.setItem(10, createToggleItem(Material.FEATHER,
                trailPlayers.getOrDefault(player.getUniqueId(), false), "粒子尾随"));
        gui.setItem(12, createToggleItem(Material.ENDER_EYE,
                invisiblePlayers.getOrDefault(player.getUniqueId(), false), "超级隐身"));
        gui.setItem(14, createGUIItem(Material.CLOCK, "§b时间控制", "调整游戏时间"));
        gui.setItem(15, createGUIItem(Material.SUNFLOWER, "§b天气控制", "改变天气状态"));
        gui.setItem(16, createGUIItem(Material.ANVIL, "§c封禁管理", "左键永久/右键临时"));
        gui.setItem(17, createGUIItem(Material.BARRIER, "§4禁言系统", "管理玩家发言权限"));

        // 填充边框
        ItemStack filler = createGUIItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i != 10 && i != 12 && i != 14 && i != 15 && i != 16 && i != 17) {
                gui.setItem(i, filler);
            }
        }
        player.openInventory(gui);
    }

    // ================= 事件处理 =================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        event.setCancelled(true);

        // 主菜单处理
        if (title.equals("§6管理面板")) {
            handleMainGUI(event);
            return;
        }

        // 二级菜单处理
        switch (title) {
            case "§b时间控制":
                handleTimeGUI(event);
                break;
            case "§b天气控制":
                handleWeatherGUI(event);
                break;
            case "§c封禁管理":
                handleBanGUI(event);
                break;
            case "§4禁言管理":
                handleMuteGUI(event);
                break;
        }
    }

    private void handleMainGUI(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        switch (item.getType()) {
            case FEATHER: toggleTrail(player); break;
            case ENDER_EYE: toggleInvisible(player); break;
            case CLOCK: openTimeGUI(player); break;
            case SUNFLOWER: openWeatherGUI(player); break;
            case ANVIL: openBanGUI(player); break;
            case BARRIER: openMuteGUI(player); break;
        }
        updateGUI(player);
    }

    // ================= 粒子系统 =================
    private void toggleTrail(Player player) {
        boolean state = !trailPlayers.getOrDefault(player.getUniqueId(), false);
        trailPlayers.put(player.getUniqueId(), state);
        if (state) startTrailEffect(player);
        player.sendMessage(state ? "§a粒子尾随已启用" : "§c粒子尾随已关闭");
    }

    private void startTrailEffect(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!trailPlayers.getOrDefault(player.getUniqueId(), false)) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 0.3, 0)
                        .add(new Vector((Math.random()-0.5)*0.4, 0.1, (Math.random()-0.5)*0.4));
                player.spawnParticle(Particle.REDSTONE, loc, 6,
                        new Particle.DustOptions(Color.fromRGB(
                                (int)(Math.random()*255),
                                (int)(Math.random()*255),
                                (int)(Math.random()*255)), 0.8f));
            }
        }.runTaskTimer(this, 0, 4);
    }

    // ================= 隐身系统 =================
    private void toggleInvisible(Player player) {
        boolean state = !invisiblePlayers.getOrDefault(player.getUniqueId(), false);
        invisiblePlayers.put(player.getUniqueId(), state);
        if (state) enableSuperHide(player);
        else disableSuperHide(player);
        player.sendMessage(state ? "§a超级隐身已启用" : "§c超级隐身已关闭");
    }

    private void enableSuperHide(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(this, player));
    }

    private void disableSuperHide(Player player) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(this, player));
    }

    // ================= 时间控制 =================
    private void openTimeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "§b时间控制");
        gui.setItem(2, createGUIItem(Material.SUNFLOWER, "§6正午", "设置时间为12:00"));
        gui.setItem(6, createGUIItem(Material.END_STONE, "§9午夜", "设置时间为00:00"));
        Arrays.asList(0,1,3,4,5,7,8).forEach(slot ->
                gui.setItem(slot, createGUIItem(Material.GRAY_STAINED_GLASS_PANE, " ")));
        player.openInventory(gui);
    }

    private void handleTimeGUI(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getSlot()) {
            case 2:
                player.getWorld().setTime(6000); // 正午
                player.sendMessage("§a时间已设置为正午");
                break;
            case 6:
                player.getWorld().setTime(18000); // 午夜
                player.sendMessage("§a时间已设置为午夜");
                break;
        }
    }

    // ================= 天气控制 =================
    private void openWeatherGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "§b天气控制");
        gui.setItem(2, createGUIItem(Material.SUNFLOWER, "§e晴天", "清除所有天气效果"));
        gui.setItem(4, createGUIItem(Material.WATER_BUCKET, "§9雨天", "开始下雨"));
        gui.setItem(6, createGUIItem(Material.LIGHTNING_ROD, "§c雷暴", "开启雷电风暴"));
        Arrays.asList(0,1,3,5,7,8).forEach(slot ->
                gui.setItem(slot, createGUIItem(Material.GRAY_STAINED_GLASS_PANE, " ")));
        player.openInventory(gui);
    }

    private void handleWeatherGUI(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getSlot()) {
            case 2:
                player.getWorld().setStorm(false);
                player.getWorld().setThundering(false);
                player.sendMessage("§a天气已设置为晴天");
                break;
            case 4:
                player.getWorld().setStorm(true);
                player.sendMessage("§a天气已设置为雨天");
                break;
            case 6:
                player.getWorld().setThundering(true);
                player.sendMessage("§a天气已设置为雷暴");
                break;
        }
    }

    // ================= 封禁系统 =================
    private void openBanGUI(Player admin) {
        Inventory gui = Bukkit.createInventory(admin, 54, "§c封禁管理");
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
        targets.remove(admin); // 排除自己

        if (targets.isEmpty()) {
            gui.setItem(22, createGUIItem(Material.BARRIER, "§c没有其他在线玩家"));
        } else {
            targets.forEach(p -> gui.addItem(createPlayerHead(p)));
        }
        admin.openInventory(gui);
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§e" + player.getName());
        meta.setLore(Arrays.asList("§7左键: 永久封禁", "§7右键: 临时封禁"));
        head.setItemMeta(meta);
        return head;
    }

    private void handleBanGUI(InventoryClickEvent event) {
        if (event.getSlot() == 22) return; // 跳过提示物品

        Player admin = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        OfflinePlayer target = meta.getOwningPlayer();

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        String targetName = target.getName();

        if (event.isLeftClick()) {
            // 永久封禁
            banList.addBan(
                    targetName,
                    "§c你已被永久封禁",
                    null, // null 表示永久
                    admin.getName()
            );
            admin.sendMessage("§a已永久封禁: " + targetName);
        } else if (event.isRightClick()) {
            // 7天临时封禁
            Date expiry = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
            banList.addBan(
                    targetName,
                    "§c你已被临时封禁7天",
                    expiry,
                    admin.getName()
            );
            admin.sendMessage("§a已临时封禁: " + targetName);
        }

        if (target.isOnline()) {
            target.getPlayer().kickPlayer("§c你已被管理员封禁");
        }
    }

    // ================= 禁言系统 =================
    private void openMuteGUI(Player admin) {
        Inventory gui = Bukkit.createInventory(admin, 54, "§4禁言管理");
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
        targets.remove(admin); // 排除自己

        if (targets.isEmpty()) {
            gui.setItem(22, createGUIItem(Material.BARRIER, "§c没有其他在线玩家"));
        } else {
            targets.forEach(p -> gui.addItem(createMuteItem(p)));
        }
        admin.openInventory(gui);
    }

    private ItemStack createMuteItem(Player player) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + player.getName());
        meta.setLore(Collections.singletonList(
                mutedPlayers.contains(player.getUniqueId()) ? "§c已禁言" : "§a可发言"));
        item.setItemMeta(meta);
        return item;
    }

    private void handleMuteGUI(InventoryClickEvent event) {
        if (event.getSlot() == 22) return; // 跳过提示物品

        Player admin = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        String targetName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(targetName);

        if (mutedPlayers.contains(target.getUniqueId())) {
            mutedPlayers.remove(target.getUniqueId());
            admin.sendMessage("§a已解禁: " + target.getName());
            target.sendMessage("§a你已被解除禁言");
        } else {
            mutedPlayers.add(target.getUniqueId());
            admin.sendMessage("§c已禁言: " + target.getName());
            target.sendMessage("§c你已被管理员禁言");
        }
        updateGUI(admin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (mutedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你已被禁言！");
        }
    }

    // ================= 通用方法 =================
    private void updateGUI(Player player) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.getOpenInventory().getTitle().equals("§6管理面板")) {
                openMainGUI(player);
            }
        }, 2);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可用！");
            return true;
        }
        if (!player.hasPermission("admin.gui")) {
            player.sendMessage("§c权限不足！");
            return true;
        }
        openMainGUI(player);
        return true;
    }
}