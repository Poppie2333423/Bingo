package de.example.probingo.game;

import de.example.probingo.BingoPlugin;
import de.example.probingo.config.ConfigData;
import de.example.probingo.config.ConfigService;
import de.example.probingo.config.TimerDirection;
import de.example.probingo.config.WinSettings;
import de.example.probingo.game.card.BingoCard;
import de.example.probingo.game.pool.ItemPool;
import de.example.probingo.gui.CardView;
import de.example.probingo.score.ScoreboardService;
import de.example.probingo.track.AcquisitionSource;
import de.example.probingo.util.MaterialNameFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;

public final class GameManager {

    public record Progress(GameMode mode,
                           int found,
                           int total,
                           int lines,
                           int requiredLines,
                           Map<TeamColor, Integer> teamLines,
                           Map<TeamColor, Integer> teamFound,
                           long elapsed,
                           Long remaining,
                           boolean countdown,
                           String seedKey) {
    }

    private final BingoPlugin plugin;
    private final ConfigService configService;
    private final ItemPool itemPool;
    private final CardView cardView;
    private final TeamManager teamManager;
    private final ScoreboardService scoreboardService;
    private final Set<UUID> lobbyPlayers = new LinkedHashSet<>();
    private GameState state = GameState.LOBBY;
    private BingoMatch currentMatch;
    private BukkitTask timerTask;
    private Long pendingSeed;
    private List<Material> pendingMaterials;

    public GameManager(BingoPlugin plugin,
                       ConfigService configService,
                       ItemPool itemPool,
                       CardView cardView,
                       TeamManager teamManager,
                       ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.configService = configService;
        this.itemPool = itemPool;
        this.cardView = cardView;
        this.teamManager = teamManager;
        this.scoreboardService = scoreboardService;
        this.scoreboardService.attach(this);
    }

    public void addToLobby(Player player) {
        if (state != GameState.LOBBY) {
            player.sendMessage(plugin.messageService().message("errors.game_running"));
            return;
        }
        if (lobbyPlayers.contains(player.getUniqueId())) {
            player.sendMessage(plugin.messageService().message("errors.already_in_lobby", Placeholder.unparsed("player", player.getName())));
            return;
        }
        lobbyPlayers.add(player.getUniqueId());
        broadcastToLobby(plugin.messageService().message("info.added_to_lobby", Placeholder.unparsed("player", player.getName())));
    }

    public void removeFromLobby(Player player) {
        if (state != GameState.LOBBY) {
            player.sendMessage(plugin.messageService().message("errors.game_running"));
            return;
        }
        if (!lobbyPlayers.remove(player.getUniqueId())) {
            player.sendMessage(plugin.messageService().message("errors.not_in_lobby", Placeholder.unparsed("player", player.getName())));
            return;
        }
        broadcastToLobby(plugin.messageService().message("info.removed_from_lobby", Placeholder.unparsed("player", player.getName())));
    }

    private void broadcastToLobby(Component component) {
        for (UUID uuid : lobbyPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(component);
            }
        }
    }

    public boolean canStart() {
        if (state != GameState.LOBBY) {
            return false;
        }
        List<Player> players = lobbyPlayers.isEmpty() ? collectEligibleOnlinePlayers() : collectEligibleLobbyPlayers();
        return players.size() >= 2;
    }

    public void startMatch(int durationMinutes, GameMode overrideMode) {
        if (state != GameState.LOBBY) {
            return;
        }
        List<Player> players = lobbyPlayers.isEmpty() ? collectEligibleOnlinePlayers() : collectEligibleLobbyPlayers();
        if (players.size() < 2) {
            Component message = plugin.messageService().message("errors.not_enough_players");
            for (Player player : players) {
                player.sendMessage(message);
            }
            return;
        }
        ConfigData data = configService.data();
        GameMode mode = overrideMode != null ? overrideMode : data.defaultMode();
        long seed;
        List<Material> materials;
        if (pendingSeed != null && pendingMaterials != null && pendingMaterials.size() >= 25) {
            seed = pendingSeed;
            materials = new ArrayList<>(pendingMaterials);
        } else {
            seed = new Random().nextLong();
            materials = itemPool.draw(seed);
        }
        pendingSeed = null;
        pendingMaterials = null;
        if (materials.size() < 25) {
            plugin.getLogger().warning("Item-Pool lieferte weniger als 25 Materialien");
            return;
        }
        int limitMinutes = durationMinutes > 0 ? durationMinutes : data.timeLimitMinutes();
        int timeLimitSeconds = limitMinutes > 0 ? limitMinutes * 60 : 0;
        currentMatch = new BingoMatch(mode, seed, timeLimitSeconds, materials);
        if (mode == GameMode.TEAMS) {
            teamManager.assignTeams(players);
            Component teamInfo = plugin.messageService().message("info.team_assignment");
            for (TeamColor color : TeamColor.values()) {
                if (teamManager.members(color).isEmpty()) {
                    continue;
                }
                BingoCard card = new BingoCard(materials);
                currentMatch.assignTeamCard(color, card);
                for (UUID uuid : teamManager.members(color)) {
                    currentMatch.assignPlayerCard(uuid, card);
                }
            }
            for (Player player : players) {
                player.sendMessage(teamInfo);
            }
        } else {
            for (Player player : players) {
                BingoCard card = new BingoCard(materials);
                currentMatch.assignPlayerCard(player.getUniqueId(), card);
            }
        }
        state = GameState.RUNNING;
        scoreboardService.startMatch(currentMatch, players);
        openCards(players);
        playSound(players, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        Component startMessage = plugin.messageService().message("info.game_started");
        for (Player player : players) {
            player.sendMessage(startMessage);
        }
        startTimer();
    }

    private List<Player> collectEligibleLobbyPlayers() {
        List<Player> players = new ArrayList<>();
        lobbyPlayers.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || !player.hasPermission("probingo.play")) {
                return true;
            }
            players.add(player);
            return false;
        });
        return players;
    }

    private List<Player> collectEligibleOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("probingo.play")) {
                players.add(player);
            }
        }
        return players;
    }

    private void openCards(Collection<Player> players) {
        for (Player player : players) {
            BingoCard card = currentMatch.cardFor(player.getUniqueId());
            if (card != null) {
                cardView.open(player, card);
            }
        }
    }

    private void startTimer() {
        cancelTimer();
        timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.RUNNING || currentMatch == null) {
                return;
            }
            if (currentMatch.timeLimitSeconds() <= 0) {
                return;
            }
            long elapsed = (System.currentTimeMillis() - currentMatch.startTime()) / 1000L;
            if (elapsed >= currentMatch.timeLimitSeconds()) {
                stopMatch(true);
            }
        }, 20L, 20L);
    }

    private void playSound(Collection<Player> players, Sound sound, float volume, float pitch) {
        for (Player player : players) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public void stopMatch(boolean announce) {
        if (state != GameState.RUNNING && state != GameState.ENDED) {
            return;
        }
        cancelTimer();
        Collection<Player> participants = getOnlineParticipants();
        if (announce) {
            Component message = plugin.messageService().message("info.game_stopped");
            participants.forEach(player -> player.sendMessage(message));
        }
        scoreboardService.stop();
        cardView.closeAll();
        state = GameState.LOBBY;
        currentMatch = null;
        teamManager.clear();
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void handleItemAcquired(Player player, ItemStack stack, AcquisitionSource source) {
        if (state != GameState.RUNNING || currentMatch == null) {
            return;
        }
        BingoCard card = currentMatch.cardFor(player.getUniqueId());
        if (card == null) {
            return;
        }
        Material material = stack.getType();
        if (!currentMatch.materials().contains(material)) {
            return;
        }
        boolean marked = card.mark(material, player.getUniqueId(), source);
        if (!marked) {
            return;
        }
        cardView.updateFor(player);
        if (currentMatch.mode() == GameMode.TEAMS) {
            TeamColor team = teamManager.teamOf(player.getUniqueId());
            if (team != null) {
                cardView.updateForIds(teamManager.members(team), card);
            }
        }
        Component message = plugin.messageService().message("info.item_found", Placeholder.unparsed("item", MaterialNameFormatter.displayName(material)));
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        checkVictory(player, card);
        scoreboardService.refresh();
    }

    private void checkVictory(Player actor, BingoCard card) {
        ConfigData data = configService.data();
        WinSettings winSettings = data.winSettings();
        boolean hasWon;
        if (winSettings.blackout()) {
            hasWon = card.blackout();
        } else {
            hasWon = card.completedLines(data.allowDiagonals()) >= winSettings.linesRequired();
        }
        if (!hasWon) {
            return;
        }
        declareWinner(actor);
    }

    private void declareWinner(Player actor) {
        if (currentMatch == null) {
            return;
        }
        state = GameState.ENDED;
        Collection<Player> participants = getOnlineParticipants();
        String winnerName;
        if (currentMatch.mode() == GameMode.TEAMS) {
            TeamColor team = teamManager.teamOf(actor.getUniqueId());
            winnerName = team != null ? (team == TeamColor.RED ? "§cRot" : "§9Blau") : actor.getName();
        } else {
            winnerName = actor.getName();
        }
        Component broadcast = plugin.messageService().message("info.win_broadcast", Placeholder.unparsed("winner", winnerName));
        for (Player player : participants) {
            player.sendMessage(broadcast);
            player.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text("§6§lBINGO!"),
                    Component.text("§e" + winnerName)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> stopMatch(false), 20L * 3);
    }

    private Collection<Player> getOnlineParticipants() {
        if (currentMatch == null) {
            return Collections.emptyList();
        }
        List<Player> players = new ArrayList<>();
        for (UUID uuid : currentMatch.participants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public GameState state() {
        return state;
    }

    public Progress progress(UUID viewer) {
        if (currentMatch == null) {
            return null;
        }
        ConfigData data = configService.data();
        BingoCard card = currentMatch.cardFor(viewer);
        int found = 0;
        int lines = 0;
        Map<TeamColor, Integer> teamLines = new EnumMap<>(TeamColor.class);
        Map<TeamColor, Integer> teamFound = new EnumMap<>(TeamColor.class);
        if (card != null) {
            found = card.foundCount();
            lines = card.completedLines(data.allowDiagonals());
        }
        if (currentMatch.mode() == GameMode.TEAMS) {
            for (TeamColor color : TeamColor.values()) {
                BingoCard teamCard = currentMatch.cardForTeam(color);
                if (teamCard == null) {
                    continue;
                }
                int teamLineCount = teamCard.completedLines(data.allowDiagonals());
                teamLines.put(color, teamLineCount);
                teamFound.put(color, teamCard.foundCount());
            }
        }
        long elapsed = (System.currentTimeMillis() - currentMatch.startTime()) / 1000L;
        Long remaining = null;
        if (currentMatch.timeLimitSeconds() > 0) {
            long remainingSeconds = Math.max(0, currentMatch.timeLimitSeconds() - elapsed);
            remaining = remainingSeconds;
        }
        boolean countdown = currentMatch.timeLimitSeconds() > 0 && configService.data().timerDirection() == TimerDirection.DOWN;
        String seedKey = Long.toHexString(currentMatch.seed()).toUpperCase(Locale.ROOT);
        if (seedKey.length() > 6) {
            seedKey = seedKey.substring(0, 6);
        }
        return new Progress(currentMatch.mode(), found, 25, lines, data.winSettings().linesRequired(), teamLines, teamFound, elapsed, remaining, countdown, seedKey);
    }

    public Set<UUID> lobbyPlayers() {
        return Collections.unmodifiableSet(lobbyPlayers);
    }

    public BingoMatch currentMatch() {
        return currentMatch;
    }

    public boolean rerollCard() {
        if (state != GameState.LOBBY) {
            return false;
        }
        long seed = new Random().nextLong();
        List<Material> materials = itemPool.draw(seed);
        if (materials.size() < 25) {
            return false;
        }
        this.pendingSeed = seed;
        this.pendingMaterials = materials;
        broadcastToLobby(plugin.messageService().message("info.rerolled"));
        return true;
    }
}
