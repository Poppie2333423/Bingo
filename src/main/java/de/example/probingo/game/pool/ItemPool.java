package de.example.probingo.game.pool;

import de.example.probingo.BingoPlugin;
import de.example.probingo.config.ConfigService;
import de.example.probingo.config.Difficulty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public final class ItemPool {

    private static final int CARD_SIZE = 25;
    private final BingoPlugin plugin;
    private final ConfigService configService;

    public ItemPool(BingoPlugin plugin) {
        this.plugin = plugin;
        this.configService = plugin.configService();
    }

    public List<Material> draw(long seed) {
        Random random = new Random(seed);
        List<WeightedMaterial> available = buildPool(configService.data().difficulty());
        Collections.shuffle(available, random);
        List<Material> result = new ArrayList<>(CARD_SIZE);
        while (result.size() < CARD_SIZE && !available.isEmpty()) {
            WeightedMaterial selected = weightedRandomPick(available, random);
            if (selected == null) {
                break;
            }
            available.remove(selected);
            result.add(selected.material());
        }
        return result;
    }

    private WeightedMaterial weightedRandomPick(List<WeightedMaterial> pool, Random random) {
        int totalWeight = pool.stream().mapToInt(WeightedMaterial::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        int pick = random.nextInt(totalWeight);
        int current = 0;
        for (WeightedMaterial entry : pool) {
            current += entry.weight();
            if (pick < current) {
                return entry;
            }
        }
        return null;
    }

    private List<WeightedMaterial> buildPool(Difficulty difficulty) {
        ConfigurationSection section = configService.itemGroupSection();
        if (section == null) {
            plugin.getLogger().log(Level.WARNING, "Keine item_groups in config.yml gefunden.");
            return Collections.emptyList();
        }
        List<WeightedMaterial> materials = new ArrayList<>();
        for (Difficulty diff : enumerateDifficulties(difficulty)) {
            ConfigurationSection diffSection = section.getConfigurationSection(diff.name());
            if (diffSection == null) {
                continue;
            }
            ConfigurationSection weighted = diffSection.getConfigurationSection("weighted");
            if (weighted == null) {
                continue;
            }
            for (String key : weighted.getKeys(false)) {
                Material material;
                try {
                    material = Material.valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Unbekanntes Material in item pool: " + key);
                    continue;
                }
                if (!isMaterialAllowed(material)) {
                    continue;
                }
                int weight = Math.max(1, weighted.getInt(key));
                materials.add(new WeightedMaterial(material, weight));
            }
        }
        return materials;
    }

    private boolean isMaterialAllowed(Material material) {
        if (configService.blacklist().contains(material)) {
            return false;
        }
        if (!configService.data().allowNether() && material.name().contains("NETHER")) {
            return false;
        }
        if (!configService.data().allowEnd() && material.name().contains("END")) {
            return false;
        }
        return material.isItem();
    }

    private List<Difficulty> enumerateDifficulties(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> List.of(Difficulty.EASY);
            case NORMAL -> List.of(Difficulty.EASY, Difficulty.NORMAL);
            case HARD -> List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
        };
    }

    private record WeightedMaterial(Material material, int weight) {
    }
}
