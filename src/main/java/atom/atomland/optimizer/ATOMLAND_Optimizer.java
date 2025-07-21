package atom.atomland.optimizer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ATOMLAND Optimizer
 *
 * @author Atom Gamer Arda
 * @version 3.0.1
 *
 * A comprehensive, all-in-one, high-performance, and customizable optimization plugin for Minecraft servers.
 * This version fixes compilation errors for Minecraft 1.21 API.
 */
public class ATOMLAND_Optimizer extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static ATOMLAND_Optimizer instance;
    private FileConfiguration config;
    private ProtocolManager protocolManager;
    private static final DecimalFormat twoDecimals = new DecimalFormat("#.##");
    private String prefix;
    private boolean entityOptimizationEnabled;
    private boolean cancelMonsterTargeting;
    private boolean preventMonsterInfighting;
    private boolean fpsHelperEnabled;
    private boolean blockHeavyParticles;
    private List<String> particleBlacklist;
    private boolean itemCleanerEnabled;
    private int itemCleanerInterval;
    private List<Integer> itemCleanerWarnings;
    private List<String> itemBlacklist;
    private boolean mobCleanerEnabled;
    private int mobCleanerInterval;
    private boolean ignoreNamedMobs;
    private boolean ignoreTamedMobs;
    private boolean ignoreInLoveMobs;
    private boolean perfMonitorEnabled;
    private double tpsThreshold;
    private double msptThreshold;
    private boolean gcOnLowTps;
    private boolean clearChunksOnLowTps;
    private boolean optimizeAiOnLowTps;
    private boolean redstoneOptimizerEnabled;
    private long redstoneMinDelay;
    private boolean unusedChunkCleanerEnabled;
    private int chunkCleanerInterval;
    private long minChunkIdleTime;
    private boolean fastChunkLoaderEnabled;
    private int preloadRadius;
    private boolean customMobAiEnabled;
    private int disableAiDistance;
    private List<String> mobAiBlacklist;
    private boolean itemCooldownsEnabled;
    private int tridentCooldown;
    private int riptideCooldown;
    private int fireworkCooldown;
    private boolean badPacketControlEnabled;
    private int packetLimit;
    private String packetWarningMsg;
    private boolean memoryOptimizerEnabled;
    private int gcInterval;
    private String gcConsoleMsg;
    private boolean explosionManagerEnabled;
    private boolean cancelExplosionEffects;
    private boolean preventExplosionBlockDamage;
    private boolean preventExplosionFire;
    private int tntLimitPerChunk;
    private int endCrystalLimitPerChunk;
    private boolean disableFallingBlocks;
    private boolean entityCullingEnabled;
    private int cullDistance;
    private List<String> cullBlacklist;
    private boolean hopperOptimizerEnabled;
    private int hopperCheckInterval;
    private final Map<UUID, Integer> packetCounts = new ConcurrentHashMap<>();
    private final Map<Location, Long> redstoneLastUpdate = new ConcurrentHashMap<>();
    private final Map<Chunk, Long> chunkLastActivity = new ConcurrentHashMap<>();
    private final Map<Chunk, Integer> tntExplosions = new ConcurrentHashMap<>();
    private final Map<Chunk, Integer> crystalExplosions = new ConcurrentHashMap<>();
    private final Map<Location, Long> hopperCooldowns = new ConcurrentHashMap<>();

    public static ATOMLAND_Optimizer getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        protocolManager = ProtocolLibrary.getProtocolManager();
        saveDefaultConfig();
        config = getConfig();
        loadConfigValues();
        registerListenersAndCommands();
        startTasks();
        registerPacketListeners();
        getLogger().info("-------------------------------------------");
        getLogger().info("ATOMLAND Optimizer v3.0.1 by Atom Gamer Arda");
        getLogger().info("Başarıyla etkinleştirildi! 1.21 API Düzeltmeleri yapıldı.");
        getLogger().info("-------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
        }
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("ATOMLAND Optimizer devre dışı bırakıldı.");
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib bulunamadı! ATOMLAND Optimizer devre dışı bırakılıyor.");
            return false;
        }
        return true;
    }

    private void loadConfigValues() {
        prefix = colorize(config.getString("prefix", "&8[&6ATOMLAND&bOptimizer&8] &r"));
        entityOptimizationEnabled = config.getBoolean("entity-optimization.enabled", true);
        cancelMonsterTargeting = config.getBoolean("entity-optimization.cancel-monster-targeting", true);
        preventMonsterInfighting = config.getBoolean("entity-optimization.prevent-monster-infighting", true);
        fpsHelperEnabled = config.getBoolean("fps-helper.enabled", true);
        blockHeavyParticles = config.getBoolean("fps-helper.block-heavy-particles", true);
        particleBlacklist = config.getStringList("fps-helper.particle-blacklist");
        itemCleanerEnabled = config.getBoolean("smart-cleaner.item-cleaner.enabled", true);
        itemCleanerInterval = config.getInt("smart-cleaner.item-cleaner.interval", 300);
        itemCleanerWarnings = config.getIntegerList("smart-cleaner.item-cleaner.broadcast-warnings");
        itemBlacklist = config.getStringList("smart-cleaner.item-cleaner.item-blacklist");
        mobCleanerEnabled = config.getBoolean("smart-cleaner.mob-cleaner.enabled", true);
        mobCleanerInterval = config.getInt("smart-cleaner.mob-cleaner.interval", 600);
        ignoreNamedMobs = config.getBoolean("smart-cleaner.mob-cleaner.ignore-named", true);
        ignoreTamedMobs = config.getBoolean("smart-cleaner.mob-cleaner.ignore-tamed", true);
        ignoreInLoveMobs = config.getBoolean("smart-cleaner.mob-cleaner.ignore-in-love", true);
        perfMonitorEnabled = config.getBoolean("performance-monitor.enabled", true);
        tpsThreshold = config.getDouble("performance-monitor.tps-threshold", 18.5);
        msptThreshold = config.getDouble("performance-monitor.mspt-threshold", 40.0);
        gcOnLowTps = config.getBoolean("performance-monitor.actions.run-garbage-collector", true);
        clearChunksOnLowTps = config.getBoolean("performance-monitor.actions.clear-unused-chunks", true);
        optimizeAiOnLowTps = config.getBoolean("performance-monitor.actions.optimize-mobs-ai", true);
        redstoneOptimizerEnabled = config.getBoolean("redstone-optimizer.enabled", true);
        redstoneMinDelay = config.getLong("redstone-optimizer.update-throttling-ms", 50);
        unusedChunkCleanerEnabled = config.getBoolean("chunk-optimizer.unused-chunk-cleaner.enabled", true);
        chunkCleanerInterval = config.getInt("chunk-optimizer.unused-chunk-cleaner.interval-seconds", 900);
        minChunkIdleTime = config.getLong("chunk-optimizer.unused-chunk-cleaner.min-idle-seconds", 300) * 1000L;
        fastChunkLoaderEnabled = config.getBoolean("chunk-optimizer.fast-chunk-loader.enabled", true);
        preloadRadius = config.getInt("chunk-optimizer.fast-chunk-loader.preload-radius", 2);
        customMobAiEnabled = config.getBoolean("custom-mob-ai.enabled", true);
        disableAiDistance = config.getInt("custom-mob-ai.disable-ai-distance", 48);
        mobAiBlacklist = config.getStringList("custom-mob-ai.ai-blacklist").stream().map(String::toUpperCase).collect(Collectors.toList());
        itemCooldownsEnabled = config.getBoolean("item-cooldowns.enabled", true);
        tridentCooldown = config.getInt("item-cooldowns.trident", 20);
        riptideCooldown = config.getInt("item-cooldowns.riptide-trident", 100);
        fireworkCooldown = config.getInt("item-cooldowns.firework-rocket", 10);
        badPacketControlEnabled = config.getBoolean("bad-packet-control.enabled", true);
        packetLimit = config.getInt("bad-packet-control.limit-per-second", 500);
        packetWarningMsg = colorize(config.getString("bad-packet-control.warning-message"));
        memoryOptimizerEnabled = config.getBoolean("memory-optimizer.enabled", true);
        gcInterval = config.getInt("memory-optimizer.gc-interval-seconds", 1800);
        gcConsoleMsg = colorize(config.getString("memory-optimizer.console-message"));
        explosionManagerEnabled = config.getBoolean("explosion-manager.enabled", true);
        cancelExplosionEffects = config.getBoolean("explosion-manager.cancel-effects", true);
        preventExplosionBlockDamage = config.getBoolean("explosion-manager.prevent-block-damage", true);
        preventExplosionFire = config.getBoolean("explosion-manager.prevent-fire", false);
        tntLimitPerChunk = config.getInt("explosion-manager.tnt-limit-per-chunk", 16);
        endCrystalLimitPerChunk = config.getInt("explosion-manager.end-crystal-limit-per-chunk", 8);
        disableFallingBlocks = config.getBoolean("disable-falling-blocks", true);
        entityCullingEnabled = config.getBoolean("entity-culling.enabled", true);
        cullDistance = config.getInt("entity-culling.cull-distance", 64);
        cullBlacklist = config.getStringList("entity-culling.cull-blacklist").stream().map(String::toUpperCase).collect(Collectors.toList());
        hopperOptimizerEnabled = config.getBoolean("hopper-optimizer.enabled", true);
        hopperCheckInterval = config.getInt("hopper-optimizer.check-interval-ticks", 8);
    }

    private void registerListenersAndCommands() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("atomoptimizer").setExecutor(this);
        getCommand("atomoptimizer").setTabCompleter(this);
    }

    private void startTasks() {
        // DÜZELTME: Bu görevler artık senkron çalışacak.
        if (itemCleanerEnabled) new SmartItemClearerTask().runTaskTimer(this, 20L, 20L);
        if (mobCleanerEnabled) new SmartMobCleanerTask().runTaskTimer(this, 200L, mobCleanerInterval * 20L);

        // Diğer görevler aynı kalabilir.
        if (perfMonitorEnabled) new PerformanceMonitorTask().runTaskTimerAsynchronously(this, 100L, 100L);
        if (badPacketControlEnabled) new PacketCounterResetTask().runTaskTimerAsynchronously(this, 20L, 20L);
        if (memoryOptimizerEnabled) new MemoryOptimizerTask().runTaskTimer(this, gcInterval * 20L, gcInterval * 20L);
        if (explosionManagerEnabled) new ExplosionLimiterResetTask().runTaskTimer(this, 1200L, 1200L);
        if (unusedChunkCleanerEnabled) new UnusedChunkCleanerTask().runTaskTimerAsynchronously(this, 600L, chunkCleanerInterval * 20L);
        if (customMobAiEnabled || entityCullingEnabled) new EntityProcessingTask().runTaskTimer(this, 40L, 40L);
    }

    private void registerPacketListeners() {
        if (fpsHelperEnabled || explosionManagerEnabled) {
            protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
                    PacketType.Play.Server.WORLD_PARTICLES,
                    PacketType.Play.Server.EXPLOSION) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if (event.isCancelled()) return;
                    PacketType type = event.getPacketType();
                    if (fpsHelperEnabled && blockHeavyParticles && type == PacketType.Play.Server.WORLD_PARTICLES) {
                        handleParticlePacket(event);
                    } else if (explosionManagerEnabled && cancelExplosionEffects && type == PacketType.Play.Server.EXPLOSION) {
                        event.setCancelled(true);
                    }
                }
            });
        }
        if (badPacketControlEnabled) {
            protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance()) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    if (event.isCancelled() || event.getPlayer() == null) return;
                    packetCounts.merge(event.getPlayer().getUniqueId(), 1, Integer::sum);
                }
            });
        }
    }

    private void handleParticlePacket(PacketEvent event) {
        try {
            WrappedParticle wrappedParticle = event.getPacket().getNewParticles().read(0);
            if (particleBlacklist.contains(wrappedParticle.getParticle().name())) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
            // Ignore
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("atomoptimizer.admin")) {
            sendMessage(sender, "&cBu komutu kullanma yetkiniz yok.");
            return true;
        }
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                performReload(sender);
                break;
            case "status":
                showStatus(sender);
                break;
            case "gc":
                forceGC(sender);
                break;
            case "clearitems":
                clearItems(sender);
                break;
            case "clearentities":
                clearEntities(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("reload", "status", "gc", "clearitems", "clearentities"),
                    new ArrayList<>());
        }
        return Collections.emptyList();
    }

    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, "&6&lATOMLAND Optimizer Komutları:");
        sendMessage(sender, "&e/ao reload &7- Eklenti ayarlarını yeniden yükler.");
        sendMessage(sender, "&e/ao status &7- Sunucu performans durumunu gösterir.");
        sendMessage(sender, "&e/ao gc &7- Sunucuda manuel olarak çöp toplayıcıyı çalıştırır.");
        sendMessage(sender, "&e/ao clearitems &7- Yerdeki tüm gereksiz eşyaları temizler.");
        sendMessage(sender, "&e/ao clearentities &7- Gereksiz tüm yaratıkları temizler.");
    }

    private void performReload(CommandSender sender) {
        reloadConfig();
        loadConfigValues();
        getServer().getScheduler().cancelTasks(this);
        startTasks();
        sendMessage(sender, "&aEklenti yapılandırması başarıyla yeniden yüklendi!");
    }

    private void showStatus(CommandSender sender) {
        double[] tps = Bukkit.getServer().getTPS();
        double mspt = Bukkit.getServer().getAverageTickTime();
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        long maxMemory = runtime.maxMemory() / 1048576L;
        int totalChunks = Bukkit.getWorlds().stream().mapToInt(w -> w.getLoadedChunks().length).sum();
        int totalEntities = Bukkit.getWorlds().stream().mapToInt(w -> w.getEntities().size()).sum();
        sendMessage(sender, "&6&lSunucu Performans Durumu");
        sendMessage(sender, "&eTPS: &f" + twoDecimals.format(tps[0]) + " (1m)");
        sendMessage(sender, "&eMSPT: &f" + twoDecimals.format(mspt));
        sendMessage(sender, "&eBellek: &f" + usedMemory + "MB / " + maxMemory + "MB");
        sendMessage(sender, "&eYüklü Chunk: &f" + totalChunks);
        sendMessage(sender, "&eToplam Varlık: &f" + totalEntities);
    }

    private void forceGC(CommandSender sender) {
        sendMessage(sender, "&eGarbage Collector (GC) manuel olarak tetikleniyor...");
        System.gc();
        sendMessage(sender, "&aGC başarıyla çalıştırıldı.");
    }

    private void clearItems(CommandSender sender) {
        int clearedCount = clearAllItems();
        sendMessage(sender, "&aBaşarıyla " + clearedCount + " adet yerdeki eşya temizlendi.");
    }

    private void clearEntities(CommandSender sender) {
        int clearedCount = clearAllMobs();
        sendMessage(sender, "&aBaşarıyla " + clearedCount + " adet yaratık temizlendi.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (entityOptimizationEnabled && cancelMonsterTargeting && event.getTarget() instanceof Player && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (entityOptimizationEnabled && preventMonsterInfighting && event.getDamager() instanceof Monster && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!redstoneOptimizerEnabled) return;
        Location loc = event.getBlock().getLocation();
        long now = System.currentTimeMillis();
        long lastUpdate = redstoneLastUpdate.getOrDefault(loc, 0L);
        if (now - lastUpdate < redstoneMinDelay) {
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
        redstoneLastUpdate.put(loc, now);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!hopperOptimizerEnabled || hopperCheckInterval <= 0) return;
        Location loc = event.getInitiator().getLocation();
        long now = System.currentTimeMillis();
        if (hopperCooldowns.containsKey(loc) && (now - hopperCooldowns.get(loc) < hopperCheckInterval * 50L)) {
            event.setCancelled(true);
            return;
        }
        hopperCooldowns.put(loc, now);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!itemCooldownsEnabled || !event.hasItem()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        Material type = item.getType();
        if (type == Material.TRIDENT) {
            int cooldown = item.containsEnchantment(Enchantment.RIPTIDE) ? riptideCooldown : tridentCooldown;
            player.setCooldown(type, cooldown);
        } else if (type == Material.FIREWORK_ROCKET) {
            player.setCooldown(type, fireworkCooldown);
        }
    }

    // ##### BU METOT DÜZELTİLDİ #####
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!explosionManagerEnabled) return;
        Chunk chunk = event.getLocation().getChunk();
        EntityType type = event.getEntityType();

        // DÜZELTME: EntityType isimleri 1.21 API'sine göre güncellendi.
        if (type == EntityType.TNT || type == EntityType.TNT_MINECART) {
            if (tntExplosions.merge(chunk, 1, Integer::sum) > tntLimitPerChunk) {
                event.setCancelled(true);
                return;
            }
            // DÜZELTME: EntityType ismi 1.21 API'sine göre güncellendi.
        } else if (type == EntityType.END_CRYSTAL) {
            if (crystalExplosions.merge(chunk, 1, Integer::sum) > endCrystalLimitPerChunk) {
                event.setCancelled(true);
                return;
            }
        }

        if (preventExplosionBlockDamage) event.blockList().clear();
        if (preventExplosionFire) event.setYield(0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallingBlock(EntityChangeBlockEvent event) {
        if (disableFallingBlocks && event.getEntityType() == EntityType.FALLING_BLOCK) {
            event.setCancelled(true);
            event.getBlock().getState().update(false, false);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!unusedChunkCleanerEnabled && !fastChunkLoaderEnabled) return;
        Chunk toChunk = event.getTo().getChunk();
        if (!event.getFrom().getChunk().equals(toChunk)) {
            chunkLastActivity.put(toChunk, System.currentTimeMillis());
            if (fastChunkLoaderEnabled) preloadChunks(event.getPlayer(), toChunk);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (fastChunkLoaderEnabled) preloadChunks(event.getPlayer(), event.getPlayer().getLocation().getChunk());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (unusedChunkCleanerEnabled) chunkLastActivity.put(event.getChunk(), System.currentTimeMillis());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (unusedChunkCleanerEnabled) chunkLastActivity.remove(event.getChunk());
    }

    private void preloadChunks(Player player, Chunk center) {
        World world = player.getWorld();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = center.getX() - preloadRadius; x <= center.getX() + preloadRadius; x++) {
                    for (int z = center.getZ() - preloadRadius; z <= center.getZ() + preloadRadius; z++) {
                        world.getChunkAtAsync(x, z);
                    }
                }
            }
        }.runTaskAsynchronously(this);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + colorize(message));
    }

    // ##### BU METOT DÜZELTİLDİ #####
    private boolean isChunkInUse(Chunk chunk) {
        // DÜZELTME: chunk.getPlayers() metodu yerine daha uyumlu olan getEntities() kullanıldı.
        if (chunk.isForceLoaded()) return true;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                return true;
            }
        }
        return false;
    }

    private int clearAllItems() {
        List<Entity> toRemove = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (!itemBlacklist.contains(item.getItemStack().getType().name())) {
                    toRemove.add(item);
                }
            }
        }
        toRemove.forEach(Entity::remove);
        return toRemove.size();
    }

    private int clearAllMobs() {
        List<LivingEntity> toRemove = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player) continue;
                if (!shouldRemoveMob(entity)) continue;
                toRemove.add(entity);
            }
        }
        toRemove.forEach(Entity::remove);
        return toRemove.size();
    }

    private boolean shouldRemoveMob(LivingEntity entity) {
        if (ignoreNamedMobs && entity.getCustomName() != null) return false;
        if (ignoreTamedMobs && entity instanceof Tameable && ((Tameable) entity).isTamed()) return false;
        if (ignoreInLoveMobs && entity instanceof Animals && ((Animals) entity).isLoveMode()) return false;
        return entity instanceof Mob;
    }

    private class SmartItemClearerTask extends BukkitRunnable {
        private int timer;
        public SmartItemClearerTask() {
            this.timer = itemCleanerInterval;
        }
        @Override
        public void run() {
            if (itemCleanerWarnings.contains(timer)) {
                String message = prefix + colorize("&eYerdeki eşyalar &c" + timer + " &esaniye içinde temizlenecek!");
                Bukkit.broadcastMessage(message);
            }
            if (timer-- <= 0) {
                int finalCount = clearAllItems();
                if (finalCount > 0) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            String message = prefix + colorize("&a" + finalCount + " adet gereksiz eşya temizlendi.");
                            Bukkit.broadcastMessage(message);
                            getLogger().info(finalCount + " adet gereksiz eşya temizlendi.");
                        }
                    }.runTask(ATOMLAND_Optimizer.this);
                }
                this.timer = itemCleanerInterval;
            }
        }
    }

    private class SmartMobCleanerTask extends BukkitRunnable {
        @Override
        public void run() {
            int finalCount = clearAllMobs();
            if (finalCount > 0) {
                getLogger().info(finalCount + " adet gereksiz canavar temizlendi.");
            }
        }
    }

    private class PerformanceMonitorTask extends BukkitRunnable {
        @Override
        public void run() {
            double currentTps = Bukkit.getServer().getTPS()[0];
            double currentMspt = Bukkit.getServer().getAverageTickTime();
            if (currentTps < tpsThreshold || currentMspt > msptThreshold) {
                String reason = currentTps < tpsThreshold ? "Düşük TPS (" + twoDecimals.format(currentTps) + ")" : "Yüksek MSPT (" + twoDecimals.format(currentMspt) + ")";
                getLogger().warning("Performans sorunu tespit edildi: " + reason + ". Otomatik optimizasyonlar başlatılıyor...");
                if (gcOnLowTps) {
                    System.gc();
                    getLogger().info("[Performans Monitörü] Düşük TPS nedeniyle Garbage Collector çalıştırıldı.");
                }
                if (clearChunksOnLowTps) {
                    new UnusedChunkCleanerTask().run();
                }
                if (optimizeAiOnLowTps) {
                    new EntityProcessingTask().run();
                }
            }
        }
    }

    private class PacketCounterResetTask extends BukkitRunnable {
        @Override
        public void run() {
            packetCounts.entrySet().removeIf(entry -> {
                if (entry.getValue() > packetLimit) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        player.sendMessage(prefix + packetWarningMsg);
                    }
                }
                return true;
            });
        }
    }

    private class MemoryOptimizerTask extends BukkitRunnable {
        @Override
        public void run() {
            System.gc();
            getLogger().info(prefix + gcConsoleMsg);
        }
    }

    private class ExplosionLimiterResetTask extends BukkitRunnable {
        @Override
        public void run() {
            tntExplosions.clear();
            crystalExplosions.clear();
        }
    }

    private class UnusedChunkCleanerTask extends BukkitRunnable {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            List<Chunk> toUnload = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (isChunkInUse(chunk)) {
                        chunkLastActivity.put(chunk, now);
                        continue;
                    }
                    if (now - chunkLastActivity.getOrDefault(chunk, 0L) > minChunkIdleTime) {
                        toUnload.add(chunk);
                    }
                }
            }
            if (!toUnload.isEmpty()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        int unloadedCount = 0;
                        for (Chunk chunk : toUnload) {
                            if (!isChunkInUse(chunk) && chunk.unload()) {
                                chunkLastActivity.remove(chunk);
                                unloadedCount++;
                            }
                        }
                        if (unloadedCount > 0) {
                            getLogger().info("[Chunk Temizleyici] " + unloadedCount + " adet kullanılmayan chunk bellekten kaldırıldı.");
                        }
                    }
                }.runTask(ATOMLAND_Optimizer.this);
            }
        }
    }

    private class EntityProcessingTask extends BukkitRunnable {
        @Override
        public void run() {
            final int aiDistSq = disableAiDistance * disableAiDistance;
            final int cullDistSq = cullDistance * cullDistance;
            for (World world : Bukkit.getWorlds()) {
                List<Player> players = world.getPlayers();
                if (players.isEmpty()) {
                    if(customMobAiEnabled) {
                        for (LivingEntity entity : world.getLivingEntities()) {
                            if (entity instanceof Mob && !mobAiBlacklist.contains(entity.getType().name())) {
                                ((Mob) entity).setAI(false);
                            }
                        }
                    }
                    continue;
                }
                List<LivingEntity> worldEntities = world.getLivingEntities();
                if (customMobAiEnabled) {
                    for (LivingEntity entity : worldEntities) {
                        if (!(entity instanceof Mob) || mobAiBlacklist.contains(entity.getType().name())) continue;
                        boolean isNearPlayer = players.stream().anyMatch(p -> p.getLocation().distanceSquared(entity.getLocation()) < aiDistSq);
                        Mob mob = (Mob) entity;
                        if (mob.hasAI() != isNearPlayer) {
                            mob.setAI(isNearPlayer);
                        }
                    }
                }
                if (entityCullingEnabled) {
                    for (Player player : players) {
                        for (LivingEntity entity : worldEntities) {
                            if (entity.equals(player) || !(entity instanceof Mob) || cullBlacklist.contains(entity.getType().name())) continue;
                            if (player.getLocation().distanceSquared(entity.getLocation()) > cullDistSq) {
                                player.hideEntity(ATOMLAND_Optimizer.this, entity);
                            } else {
                                player.showEntity(ATOMLAND_Optimizer.this, entity);
                            }
                        }
                    }
                }
            }
        }
    }
}