package tfcr.data;

/**
 * An Enum that represents different types of terrain.
 *
 * These are essentially placeholder biomes used in worldgen, and need a corresponding
 * temperature/precipitation value to determine the final biome type.
 */
public enum TerrainType {
    DEEP_OCEAN(-1.8f, 0.1f), // Based on deep cold ocean biome
    OCEAN(-1f, 0.1f), // Based on ocean biome
    // Shallow ocean?
    BEACH(0f, 0.025f), // Based on beach biome
    CLIFF(0.1f, 0.8f), // Based on stone shore biome
    RIVER(-0.5f, 0.0f), // Based on river biome
    // Lake?
    FLAT(0.2f, 0.05f), // Based on plains
    SMALL_HILLS(0.5f, 0.2f), // Based on forest
    BIG_HILLS(0.8f, 0.4f), // Based on dark forest hills biome
    MOUNTAINS(1.5f, 0.75f); // Based on mountains biome
    // Highland plains? High depth, but low scale

    public float depth;
    public float scale;

    TerrainType(float depth, float scale) {
        this.depth = depth;
        this.scale = scale;
    }
}
