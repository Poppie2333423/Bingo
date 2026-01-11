package com.example.missilewars;

import org.bukkit.block.BlockFace;

public enum BlockFaceDirection {
    NORTH(BlockFace.NORTH),
    EAST(BlockFace.EAST),
    SOUTH(BlockFace.SOUTH),
    WEST(BlockFace.WEST);

    private final BlockFace face;

    BlockFaceDirection(BlockFace face) {
        this.face = face;
    }

    public BlockFace toFace() {
        return face;
    }

    public BlockFaceDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public BlockFaceDirection rotateClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public int rotationsTo(BlockFaceDirection target) {
        if (this == target) {
            return 0;
        }
        if (rotateClockwise() == target) {
            return 1;
        }
        if (rotateClockwise().rotateClockwise() == target) {
            return 2;
        }
        if (rotateClockwise().rotateClockwise().rotateClockwise() == target) {
            return 3;
        }
        return 0;
    }

    public static BlockFaceDirection fromString(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase()) {
            case "north" -> NORTH;
            case "east" -> EAST;
            case "south" -> SOUTH;
            case "west" -> WEST;
            default -> null;
        };
    }

    public static BlockFaceDirection fromFace(BlockFace face) {
        if (face == null) {
            return NORTH;
        }
        return switch (face) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }
}
