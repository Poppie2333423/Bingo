package de.example.probingo.game.card;

import de.example.probingo.track.AcquisitionSource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;

public final class BingoCard {

    private static final int SIZE = 5;
    private final List<BingoCell> cells;

    public BingoCard(List<Material> materials) {
        if (materials.size() < SIZE * SIZE) {
            throw new IllegalArgumentException("Es werden mindestens 25 Materialien benötigt");
        }
        this.cells = Collections.unmodifiableList(materials.subList(0, SIZE * SIZE).stream()
                .map(BingoCell::new)
                .toList());
    }

    public List<BingoCell> cells() {
        return cells;
    }

    public boolean mark(Material material, UUID playerId, AcquisitionSource source) {
        for (BingoCell cell : cells) {
            if (cell.material() == material) {
                if (cell.isCompleted()) {
                    return false;
                }
                cell.mark(playerId, source, Instant.now());
                return true;
            }
        }
        return false;
    }

    public int foundCount() {
        int count = 0;
        for (BingoCell cell : cells) {
            if (cell.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public boolean blackout() {
        return foundCount() >= SIZE * SIZE;
    }

    public int completedLines(boolean includeDiagonals) {
        int lines = 0;
        for (int row = 0; row < SIZE; row++) {
            if (lineComplete(row * SIZE, 1)) {
                lines++;
            }
        }
        for (int col = 0; col < SIZE; col++) {
            if (lineComplete(col, SIZE)) {
                lines++;
            }
        }
        if (includeDiagonals) {
            if (lineComplete(0, SIZE + 1)) {
                lines++;
            }
            if (lineComplete(SIZE - 1, SIZE - 1)) {
                lines++;
            }
        }
        return lines;
    }

    private boolean lineComplete(int start, int step) {
        for (int i = 0; i < SIZE; i++) {
            if (!cells.get(start + step * i).isCompleted()) {
                return false;
            }
        }
        return true;
    }
}
