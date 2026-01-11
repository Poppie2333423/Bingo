package com.example.missilewars;

import java.util.List;

import org.bukkit.Material;

public record RocketDefinition(
    String name,
    Material eggMaterial,
    BlockFaceDirection direction,
    Material pistonMaterial,
    List<RocketBlock> blocks
) {
}
