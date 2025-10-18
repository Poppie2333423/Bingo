package de.example.probingo.game;

import de.example.probingo.game.card.BingoCard;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;

public final class BingoMatch {

    private final GameMode mode;
    private final long seed;
    private final long startTime;
    private final int timeLimitSeconds;
    private final Map<UUID, BingoCard> playerCards = new HashMap<>();
    private final Map<TeamColor, BingoCard> teamCards = new EnumMap<>(TeamColor.class);
    private final List<Material> materials;

    public BingoMatch(GameMode mode, long seed, int timeLimitSeconds, List<Material> materials) {
        this.mode = mode;
        this.seed = seed;
        this.startTime = System.currentTimeMillis();
        this.timeLimitSeconds = timeLimitSeconds;
        this.materials = List.copyOf(materials);
    }

    public void assignPlayerCard(UUID uuid, BingoCard card) {
        playerCards.put(uuid, card);
    }

    public void assignTeamCard(TeamColor color, BingoCard card) {
        teamCards.put(color, card);
    }

    public GameMode mode() {
        return mode;
    }

    public long seed() {
        return seed;
    }

    public long startTime() {
        return startTime;
    }

    public int timeLimitSeconds() {
        return timeLimitSeconds;
    }

    public BingoCard cardFor(UUID uuid) {
        return playerCards.get(uuid);
    }

    public BingoCard cardForTeam(TeamColor color) {
        return teamCards.get(color);
    }

    public List<Material> materials() {
        return materials;
    }

    public Set<UUID> participants() {
        return Collections.unmodifiableSet(playerCards.keySet());
    }
}
