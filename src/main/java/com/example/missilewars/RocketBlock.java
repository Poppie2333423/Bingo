package com.example.missilewars;

import org.bukkit.Material;
import org.bukkit.util.BlockVector;

public record RocketBlock(BlockVector offset, Material material, String blockData) {
}
