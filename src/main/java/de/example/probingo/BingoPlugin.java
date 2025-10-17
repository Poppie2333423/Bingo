package de.example.probingo;

import de.example.probingo.command.BingoCommand;
import de.example.probingo.config.ConfigService;
import de.example.probingo.config.MessageService;
import de.example.probingo.game.GameManager;
import de.example.probingo.gui.CardView;
import de.example.probingo.score.ScoreboardService;
import de.example.probingo.track.ItemTracker;
import de.example.probingo.game.TeamManager;
import de.example.probingo.game.pool.ItemPool;
import org.bukkit.plugin.java.JavaPlugin;

public final class BingoPlugin extends JavaPlugin {

    private ConfigService configService;
    private MessageService messageService;
    private ItemPool itemPool;
    private GameManager gameManager;
    private CardView cardView;
    private TeamManager teamManager;
    private ScoreboardService scoreboardService;
    private ItemTracker itemTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configService = new ConfigService(this);
        this.messageService = new MessageService(this);
        this.itemPool = new ItemPool(this);
        this.teamManager = new TeamManager();
        this.cardView = new CardView();
        this.scoreboardService = new ScoreboardService(this, teamManager);
        this.gameManager = new GameManager(this, configService, itemPool, cardView, teamManager, scoreboardService);
        this.itemTracker = new ItemTracker(gameManager);

        getServer().getPluginManager().registerEvents(cardView, this);
        getServer().getPluginManager().registerEvents(itemTracker, this);

        BingoCommand bingoCommand = new BingoCommand(gameManager, messageService, cardView);
        getCommand("bingo").setExecutor(bingoCommand);
        getCommand("bingo").setTabCompleter(bingoCommand);

        getLogger().info("ProBingo erfolgreich geladen.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopMatch(false);
        }
        if (scoreboardService != null) {
            scoreboardService.clear();
        }
    }

    public ConfigService configService() {
        return configService;
    }

    public MessageService messageService() {
        return messageService;
    }

    public ItemPool itemPool() {
        return itemPool;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public CardView cardView() {
        return cardView;
    }

    public TeamManager teamManager() {
        return teamManager;
    }

    public ScoreboardService scoreboardService() {
        return scoreboardService;
    }
}
