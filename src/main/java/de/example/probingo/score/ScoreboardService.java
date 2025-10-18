package de.example.probingo.score;

import de.example.probingo.BingoPlugin;
import de.example.probingo.game.BingoMatch;
import de.example.probingo.game.GameManager;
import de.example.probingo.game.GameMode;
import de.example.probingo.game.TeamColor;
import de.example.probingo.game.TeamManager;
import de.example.probingo.util.TimeFormats;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class ScoreboardService {

    private static final int TOTAL_LINES = 8;

    private final BingoPlugin plugin;
    private final Map<UUID, PlayerBoard> boards = new HashMap<>();
    private GameManager gameManager;
    private BukkitTask task;
    private final TeamManager teamManager;

    public ScoreboardService(BingoPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void attach(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void startMatch(BingoMatch match, List<Player> players) {
        clear();
        for (Player player : players) {
            PlayerBoard board = createBoard();
            teamManager.applyToScoreboard(board.scoreboard());
            boards.put(player.getUniqueId(), board);
            player.setScoreboard(board.scoreboard());
        }
        refresh();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refresh, 10L, 10L);
    }

    public void refresh() {
        for (Map.Entry<UUID, PlayerBoard> entry : boards.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerBoard board = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            GameManager.Progress progress = gameManager.progress(uuid);
            if (progress == null) {
                continue;
            }
            List<Component> lines = buildLines(uuid, progress);
            board.apply(lines);
            player.setScoreboard(board.scoreboard());
        }
    }

    private List<Component> buildLines(UUID viewer, GameManager.Progress progress) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("§7────────────"));
        lines.add(Component.text("Modus: §f" + (progress.mode() == GameMode.TEAMS ? "Teams" : "FFA")));
        long timeValue = progress.countdown() && progress.remaining() != null ? progress.remaining() : progress.elapsed();
        String arrow = progress.countdown() ? "▼ " : "▲ ";
        lines.add(Component.text("Zeit: §f" + arrow + TimeFormats.clock(timeValue)));
        lines.add(Component.text("Gefunden: §a" + progress.found() + "§7/§f" + progress.total()));
        lines.add(Component.text("Reihen: §a" + progress.lines() + "§7/§f" + progress.requiredLines()));
        if (progress.mode() == GameMode.TEAMS) {
            int red = progress.teamLines().getOrDefault(TeamColor.RED, 0);
            int blue = progress.teamLines().getOrDefault(TeamColor.BLUE, 0);
            lines.add(Component.text("§cRot: §f" + red));
            lines.add(Component.text("§9Blau: §f" + blue));
        } else {
            BingoMatch match = gameManager.currentMatch();
            int participantCount = match != null ? match.participants().size() : 0;
            lines.add(Component.text("Teilnehmer: §f" + participantCount));
        }
        lines.add(Component.text("Seed: §7" + progress.seedKey()));
        while (lines.size() < TOTAL_LINES) {
            lines.add(Component.text(" "));
        }
        return lines;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (UUID uuid : boards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setScoreboard(main);
            }
        }
        boards.clear();
    }

    public void clear() {
        stop();
    }

    private PlayerBoard createBoard() {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("probingo", "dummy", Component.text("§b§lBINGO"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Map<Integer, Team> lines = new HashMap<>();
        for (int i = 0; i < TOTAL_LINES; i++) {
            String entry = "§" + Integer.toHexString(i);
            Team team = board.registerNewTeam("probingo_line_" + i);
            team.addEntry(entry);
            objective.getScore(entry).setScore(TOTAL_LINES - i);
            lines.put(i, team);
        }
        return new PlayerBoard(board, lines);
    }

    private static final class PlayerBoard {
        private final Scoreboard scoreboard;
        private final Map<Integer, Team> lines;

        private PlayerBoard(Scoreboard scoreboard, Map<Integer, Team> lines) {
            this.scoreboard = scoreboard;
            this.lines = lines;
        }

        public Scoreboard scoreboard() {
            return scoreboard;
        }

        public void apply(List<Component> entries) {
            for (int i = 0; i < entries.size() && i < lines.size(); i++) {
                Team team = lines.get(i);
                if (team != null) {
                    team.prefix(entries.get(i));
                    team.suffix(Component.empty());
                }
            }
        }
    }
}
