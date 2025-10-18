package de.example.probingo.config;

import de.example.probingo.BingoPlugin;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

public final class MessageService {

    private final BingoPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Component> cache = new ConcurrentHashMap<>();
    private final String prefix;
    private final FileConfiguration config;

    public MessageService(BingoPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.configService().loadMessagesConfig();
        this.prefix = config.getString("prefix", "");
    }

    public Component message(String key, TagResolver... placeholders) {
        String path = key;
        String raw = config.getString(path, "<gray>" + MessageFormat.format("Missing message {0}", path) + "</gray>");
        TagResolver resolver = TagResolver.resolver(placeholders);
        Component base = miniMessage.deserialize(raw, resolver);
        if (!prefix.isEmpty()) {
            Component prefixComponent = cache.computeIfAbsent("prefix", k -> miniMessage.deserialize(prefix));
            return prefixComponent.append(Component.space()).append(base);
        }
        return base;
    }

    public Component plain(String raw, TagResolver... placeholders) {
        return miniMessage.deserialize(raw, TagResolver.resolver(placeholders));
    }
}
