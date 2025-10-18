package de.example.probingo.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public final class TeamManager {

    private final Map<TeamColor, Set<UUID>> members = new EnumMap<>(TeamColor.class);

    public TeamManager() {
        for (TeamColor color : TeamColor.values()) {
            members.put(color, new LinkedHashSet<>());
        }
    }

    public void assignTeams(Collection<Player> players) {
        clear();
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (Player player : sorted) {
            TeamColor team = teamWithFewestMembers();
            members.get(team).add(player.getUniqueId());
        }
    }

    private TeamColor teamWithFewestMembers() {
        TeamColor current = TeamColor.RED;
        for (TeamColor color : TeamColor.values()) {
            if (members.get(color).size() < members.get(current).size()) {
                current = color;
            }
        }
        return current;
    }

    public void applyToScoreboard(Scoreboard scoreboard) {
        for (TeamColor color : TeamColor.values()) {
            org.bukkit.scoreboard.Team team = scoreboard.getTeam("probingo_" + color.name().toLowerCase());
            if (team == null) {
                team = scoreboard.registerNewTeam("probingo_" + color.name().toLowerCase());
            }
            team.color(color.color());
            team.prefix(net.kyori.adventure.text.Component.empty());
            team.setAllowFriendlyFire(false);
            for (UUID memberId : members.get(color)) {
                Player player = Bukkit.getPlayer(memberId);
                if (player != null) {
                    team.addEntry(player.getName());
                }
            }
        }
    }

    public Set<UUID> members(TeamColor color) {
        return Collections.unmodifiableSet(members.get(color));
    }

    public TeamColor teamOf(UUID uuid) {
        for (Map.Entry<TeamColor, Set<UUID>> entry : members.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void clear() {
        for (Set<UUID> set : members.values()) {
            set.clear();
        }
    }
}
