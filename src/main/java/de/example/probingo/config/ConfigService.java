package de.example.probingo.config;

import de.example.probingo.BingoPlugin;
import de.example.probingo.game.GameMode;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigService {

    private final BingoPlugin plugin;
    private ConfigData configData;
    private Set<Material> blacklist;

    public ConfigService(BingoPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        GameMode mode = GameMode.fromString(config.getString("mode"), GameMode.TEAMS);
        int timeLimit = Math.max(0, config.getInt("time_limit_minutes", 0));
        boolean allowDiagonals = config.getBoolean("allow_diagonals", true);
        Difficulty difficulty = Difficulty.fromString(config.getString("difficulty"), Difficulty.NORMAL);
        boolean allowNether = config.getBoolean("allow_nether", false);
        boolean allowEnd = config.getBoolean("allow_end", false);
        boolean allowTeamChat = config.getBoolean("allow_team_chat", true);
        TimerDirection direction = TimerDirection.fromString(config.getString("default_timer_direction"), TimerDirection.DOWN);
        ConfigurationSection winSection = config.getConfigurationSection("win_conditions");
        boolean blackout = winSection != null && winSection.getBoolean("blackout", false);
        int linesRequired = winSection != null ? winSection.getInt("lines_required", 1) : 1;
        this.configData = new ConfigData(mode, timeLimit, allowDiagonals, difficulty, allowNether, allowEnd, allowTeamChat, direction, new WinSettings(blackout, linesRequired));
        this.blacklist = loadBlacklist(config);
    }

    private Set<Material> loadBlacklist(FileConfiguration config) {
        Set<Material> set = new HashSet<>();
        List<String> values = config.getStringList("blacklist");
        for (String value : values) {
            try {
                Material material = Material.valueOf(value.toUpperCase(Locale.ROOT));
                set.add(material);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Set.copyOf(set);
    }

    public ConfigData data() {
        return configData;
    }

    public Set<Material> blacklist() {
        return blacklist;
    }

    public FileConfiguration loadMessagesConfig() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public ConfigurationSection itemGroupSection() {
        return plugin.getConfig().getConfigurationSection("item_groups");
    }
}
