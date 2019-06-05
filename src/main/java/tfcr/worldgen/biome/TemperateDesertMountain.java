package tfcr.worldgen.biome;

import net.minecraft.init.Blocks;
import net.minecraft.world.gen.surfacebuilders.CompositeSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilderConfig;
import tfcr.TFCR;
import tfcr.data.TerrainType;

/**
 * Concrete biome implementation.
 * Temperature: [-30-30], Precipitation: [0-10]
 */
public class TemperateDesertMountain extends BaseTFCRBiome {
    public TemperateDesertMountain() {
        super(-30, 30, 0, 10, TerrainType.MOUNTAINS,
                new BiomeBuilder()
                        .surfaceBuilder(new CompositeSurfaceBuilder<>(MESA_BRYCE_SURACE_BUILDER,
                                new SurfaceBuilderConfig(
                                Blocks.SAND.getDefaultState(),
                                Blocks.WHITE_TERRACOTTA.getDefaultState(),
                                Blocks.GRAVEL.getDefaultState())))
                        .category(Category.DESERT)
                        .waterColor(4159204)
                        .waterFogColor(329011)
        );
        this.setRegistryName(TFCR.MODID, "temperate_desert_mountain");
    }
}
