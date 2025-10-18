package de.example.probingo.game.card;

import de.example.probingo.track.AcquisitionSource;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;

public final class BingoCell {

    private final Material material;
    private UUID collector;
    private AcquisitionSource source;
    private Instant timestamp;

    public BingoCell(Material material) {
        this.material = material;
    }

    public Material material() {
        return material;
    }

    public boolean isCompleted() {
        return collector != null;
    }

    public UUID collector() {
        return collector;
    }

    public AcquisitionSource source() {
        return source;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public void mark(UUID collector, AcquisitionSource source, Instant timestamp) {
        if (isCompleted()) {
            return;
        }
        this.collector = collector;
        this.source = source;
        this.timestamp = timestamp;
    }
}
