package de.example.probingo.command;

import de.example.probingo.config.MessageService;
import de.example.probingo.game.GameManager;
import de.example.probingo.game.GameMode;
import de.example.probingo.game.GameState;
import de.example.probingo.gui.CardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BingoCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final MessageService messages;
    private final CardView cardView;

    public BingoCommand(GameManager gameManager,
                        MessageService messages,
                        CardView cardView) {
        this.gameManager = gameManager;
        this.messages = messages;
        this.cardView = cardView;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/bingo <start|stop|card|reroll|add|remove>"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "card" -> handleCard(sender);
            case "reroll" -> handleReroll(sender);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> {
                sender.sendMessage(Component.text("/bingo <start|stop|card|reroll|add|remove>"));
                yield true;
            }
        };
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("probingo.admin")) {
            sender.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (gameManager.state() != GameState.LOBBY) {
            sender.sendMessage(messages.message("errors.game_running"));
            return true;
        }
        int duration = 0;
        GameMode mode = null;
        if (args.length >= 2) {
            try {
                duration = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length >= 3) {
            mode = GameMode.fromString(args[2], null);
        }
        if (!gameManager.canStart()) {
            sender.sendMessage(messages.message("errors.not_enough_players"));
            return true;
        }
        gameManager.startMatch(duration, mode);
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("probingo.admin")) {
            sender.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (gameManager.state() != GameState.RUNNING) {
            sender.sendMessage(messages.message("errors.not_running"));
            return true;
        }
        gameManager.stopMatch(true);
        return true;
    }

    private boolean handleCard(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können dieses Kommando nutzen."));
            return true;
        }
        if (!player.hasPermission("probingo.play")) {
            player.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (gameManager.state() != GameState.RUNNING) {
            player.sendMessage(messages.message("errors.not_running"));
            return true;
        }
        if (gameManager.currentMatch() == null) {
            player.sendMessage(messages.message("errors.not_running"));
            return true;
        }
        var card = gameManager.currentMatch().cardFor(player.getUniqueId());
        if (card == null) {
            player.sendMessage(messages.message("errors.not_running"));
            return true;
        }
        cardView.open(player, card);
        player.sendMessage(messages.message("info.card_opened"));
        return true;
    }

    private boolean handleReroll(CommandSender sender) {
        if (!sender.hasPermission("probingo.admin")) {
            sender.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (gameManager.rerollCard()) {
            sender.sendMessage(messages.message("info.rerolled"));
        } else {
            sender.sendMessage(messages.message("errors.reroll_locked"));
        }
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("probingo.admin")) {
            sender.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/bingo add <Spieler>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messages.message("errors.unknown_player"));
            return true;
        }
        gameManager.addToLobby(target);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("probingo.admin")) {
            sender.sendMessage(messages.message("errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/bingo remove <Spieler>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messages.message("errors.unknown_player"));
            return true;
        }
        gameManager.removeFromLobby(target);
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "card", "reroll", "add", "remove");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return List.of("teams", "ffa");
        }
        return List.of();
    }
}
