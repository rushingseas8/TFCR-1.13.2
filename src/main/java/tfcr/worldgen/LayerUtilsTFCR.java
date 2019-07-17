package tfcr.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IContext;
import net.minecraft.world.gen.IContextExtended;
import net.minecraft.world.gen.LazyAreaLayerContext;
import net.minecraft.world.gen.OverworldGenSettings;
import net.minecraft.world.gen.area.AreaDimension;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.area.IAreaFactory;
import net.minecraft.world.gen.area.LazyArea;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.traits.IAreaTransformer0;
import net.minecraft.world.gen.layer.traits.IAreaTransformer1;
import tfcr.data.TerrainType;
import tfcr.worldgen.genlayer.GenLayer;
import tfcr.worldgen.genlayer.GenLayerAddIsland;
import tfcr.worldgen.genlayer.GenLayerAddMountain;
import tfcr.worldgen.genlayer.GenLayerDeepOcean;
import tfcr.worldgen.genlayer.GenLayerEqualize;
import tfcr.worldgen.genlayer.GenLayerHills;
import tfcr.worldgen.genlayer.GenLayerIsland;
import tfcr.worldgen.genlayer.GenLayerRemoveTooMuchOcean;
import tfcr.worldgen.genlayer.GenLayerRiver;
import tfcr.worldgen.genlayer.GenLayerRiverInit;
import tfcr.worldgen.genlayer.GenLayerRiverMask;
import tfcr.worldgen.genlayer.GenLayerShore;
import tfcr.worldgen.genlayer.GenLayerTempPrecip;
import tfcr.worldgen.genlayer.GenLayerTempPrecipMask;

import java.util.Random;
import java.util.function.LongFunction;

/**
 * A class closely based on LayerUtils in vanilla.
 *
 * Handles the custom biome worldgen for TFCR.
 */
public class LayerUtilsTFCR {


    // Constants related to the ordinal of each TerrainType.
    // These act as indices for the placeholder placeholderBiomes.
    public static final int DEEP_OCEAN = TerrainType.DEEP_OCEAN.ordinal();
    public static final int OCEAN = TerrainType.OCEAN.ordinal();
    public static final int BEACH = TerrainType.BEACH.ordinal();
    public static final int CLIFF = TerrainType.CLIFF.ordinal();
    public static final int RIVER = TerrainType.RIVER.ordinal();
    public static final int FLAT = TerrainType.FLAT.ordinal();
    public static final int SMALL_HILLS = TerrainType.SMALL_HILLS.ordinal();
    public static final int BIG_HILLS = TerrainType.BIG_HILLS.ordinal();
    public static final int MOUNTAINS = TerrainType.MOUNTAINS.ordinal();

    public static boolean isOcean(int terrainType) {
        return terrainType == OCEAN || terrainType == DEEP_OCEAN;
    }

    public static boolean isShallowOcean(int terrainType) {
        return terrainType == OCEAN;
    }

    public static boolean isWater(int terrainType) {
        return terrainType == OCEAN || terrainType == DEEP_OCEAN || terrainType == RIVER || terrainType == BEACH || terrainType == CLIFF;
    }

    /**
     * True if any of the surrounding tiles have any ocean terrain
     * @param south
     * @param east
     * @param north
     * @param west
     * @return
     */
    public static boolean hasOcean(int south, int east, int north, int west) {
        return LayerUtilsTFCR.isOcean(south) ||
                LayerUtilsTFCR.isOcean(east) ||
                LayerUtilsTFCR.isOcean(west) ||
                LayerUtilsTFCR.isOcean(north);
    }
    /**
     * True if any of the surrounding tiles have a MOUNTAINS terrain
     * @param south
     * @param east
     * @param north
     * @param west
     * @return
     */
    public static boolean hasMountain(int south, int east, int north, int west) {
        return south == LayerUtilsTFCR.MOUNTAINS ||
                east == LayerUtilsTFCR.MOUNTAINS ||
                north == LayerUtilsTFCR.MOUNTAINS ||
                west == LayerUtilsTFCR.MOUNTAINS;
    }

    /**
     * True if any of the surrounding tiles have a BIG_HILLS terrain
     * @param south
     * @param east
     * @param north
     * @param west
     * @return
     */
    public static boolean hasBigHill(int south, int east, int north, int west) {
        return south == LayerUtilsTFCR.BIG_HILLS ||
                east == LayerUtilsTFCR.BIG_HILLS ||
                north == LayerUtilsTFCR.BIG_HILLS ||
                west == LayerUtilsTFCR.BIG_HILLS;
    }

    /**
     * Repeats a given GenLayer a number of times.
     * @param seed The base seed for the RNG. We start at this value, and increment it by 1 each loop.
     * @param transform The GenLayer we will apply
     * @param base The GenLayer we apply transform to repeatedly
     * @param count The number of times to repeat the action.
     * @param contextFactory
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T extends IArea, C extends IContextExtended<T>> IAreaFactory<T> repeat(long seed, IAreaTransformer1 transform, IAreaFactory<T> base, int count, LongFunction<C> contextFactory) {
        IAreaFactory<T> iareafactory = base;

        for (int i = 0; i < count; ++i) {
            iareafactory = transform.apply(contextFactory.apply(seed + (long)i), iareafactory);
        }

        return iareafactory;
    }

    // Directly inspired by LayerUtils
    /**
     * A method that handles assigning placeholderBiomes in the world.
     *
     * We have to recreate a few of the GenLayer* classes from Vanilla, since those
     * return hard-coded indices for an overworld Biome array. We instead return
     * integers corresponding to different TerrainTypes, which will later be
     * resolved into actual placeholderBiomes based off of a temperature/precipitation map.
     *
     * @param worldTypeIn
     * @param settings
     * @param contextFactory
     * @param <T>
     * @param <C>
     * @return
     */
    private static <T extends IArea, C extends IContextExtended<T>> ImmutableList<IAreaFactory<T>> buildOverworldProcedure(long seed, WorldType worldTypeIn, OverworldGenSettings settings, LongFunction<C> contextFactory) {
        // Basic worldgen. In Vanilla this section handles plains/forest islands and oceans
        // iareafactory --> baseAreaFactory
        IAreaFactory<T> baseAreaFactory = GenLayerIsland.INSTANCE.apply(contextFactory.apply(1L));
        baseAreaFactory = GenLayerZoom.FUZZY.apply(contextFactory.apply(2000L), baseAreaFactory); // Zoom out 2x, fuzz
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(1L), baseAreaFactory); // Add small hill islands
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2001L), baseAreaFactory); // Zoom out 2x, normal
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory);  // Add 3 layers of small hill islands
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(50L), baseAreaFactory);
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(70L), baseAreaFactory);
        baseAreaFactory = GenLayerRemoveTooMuchOcean.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory); // If a region is all ocean, there's a 50% chance to flip it to flat.
        // Skip the ocean-related code, since we don't care about ocean temp yet
        // GenLayerAddSnow actually adds hilly + mountainous regions. So renamed to GenLayerAddMountain.
        baseAreaFactory = GenLayerAddMountain.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory);
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), baseAreaFactory); // More small hill islands
        // GenLayerEdge plays a role here. It looks like it messes with temperature (adding deserts/mountains),
        // and then adds a rare chance of having special placeholderBiomes. We don't care about that.
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2002L), baseAreaFactory); // Zoom out 2x
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2003L), baseAreaFactory); // And again, for a total of 4x
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(4L), baseAreaFactory); // More small hill islands
        // Don't care about mushroom islands
        baseAreaFactory = GenLayerDeepOcean.INSTANCE.apply(contextFactory.apply(4L), baseAreaFactory); // Turn center ocean tiles into deep ocean

        // Custom. Tries to normalize land. 2 passes to potentially fully normalize mountain boundaries.
        baseAreaFactory = repeat(16L, GenLayerEqualize.INSTANCE, baseAreaFactory, 2, contextFactory);
        baseAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, baseAreaFactory, 0, contextFactory); // no-op?

        // River and biome size setup
        int biomeSize = 4;
        int riverSize = 4;
//        if (settings != null) {
//            biomeSize = settings.getBiomeSize();
//            riverSize = settings.getRiverSize();
//        }

        // Temp/precip layer.
        IAreaFactory<T> tempPrecipLayer = new GenLayerTempPrecip(new Random(seed)).apply(contextFactory.apply(17L));
        // Zoom out the temp/precip layer to be roughly the size we need. Without scaling the noise,
        // the isotherms would change roughly every block. So if we zoom 10 times total, we get a scale
        // of 2^10 = 1024 blocks between isotherm changes. This gives us roughly the desired biome size.
        // We divide by ~500 in the noisegen, and apply fuzzy zooming on top to get to this value.
        tempPrecipLayer = GenLayerZoom.FUZZY.apply(contextFactory.apply(2004L), tempPrecipLayer);

        // lvt_7_1_ --> riverAreaFactory
        IAreaFactory<T> riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, baseAreaFactory, 0, contextFactory); // This is a no-op?
        riverAreaFactory = GenLayerRiverInit.INSTANCE.apply(contextFactory.apply(100L), riverAreaFactory);

        // Add Biomes using getBiomeLayer. The TFCR variation uses TerrainType-based logic.
        // lvt_8_1_ --> biomesAreaFactory
        IAreaFactory<T> biomesAreaFactory = worldTypeIn.getBiomeLayer(baseAreaFactory, settings, contextFactory);

        // lvt_9_1_ --> zoomedRiverInitAreaFactory
//        IAreaFactory<T> zoomedRiverInitAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, 2, contextFactory);
        biomesAreaFactory = GenLayerHills.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Random chance to raise land (modified from orig)
        riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, 2, contextFactory);
        riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, riverSize, contextFactory);
        riverAreaFactory = GenLayerRiver.INSTANCE.apply(contextFactory.apply(1L), riverAreaFactory); // Add rivers
        riverAreaFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), riverAreaFactory); // Smooth the region

        // Expand the placeholderBiomes as needed; based on biomeSize parameter
        for (int zoomIteration = 0; zoomIteration < biomeSize; zoomIteration++) {
            biomesAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(1000L + zoomIteration), biomesAreaFactory);
            if (zoomIteration == 0) {
                biomesAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), biomesAreaFactory); // Add 1 pass of small hill islands
            }

            if (zoomIteration == 1 || biomeSize == 1) {
                biomesAreaFactory = GenLayerShore.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Add beaches
            }
        }

        biomesAreaFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Smooth after zoom
        biomesAreaFactory = GenLayerRiverMask.INSTANCE.apply(contextFactory.apply(100L), biomesAreaFactory, riverAreaFactory); // Mix in the rivers

        // Apply the temp/precip map on top.
        biomesAreaFactory = GenLayerTempPrecipMask.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory, tempPrecipLayer);

        // No ocean temperature mixing at this time

        // Voronoi zoom out
        IAreaFactory<T> voronoiZoomed = GenLayerVoronoiZoom.INSTANCE.apply(contextFactory.apply(10L), biomesAreaFactory);

        // TODO Add a mask layer for applying temp/precip transforms. Similar to GenLayerRiverMask, but
        //  using a Perlin noise based map that's been fuzzy zoomed out to the right scale.

        return ImmutableList.of(biomesAreaFactory, voronoiZoomed, biomesAreaFactory);
    }

    /**
     * TODO placeholder
     * A very basic placeholder worldgen. Primary biome is forest (specifically, TemperateConiferousBiome)
     * but with the addition of oceans, rivers, beaches, and cliff biomes (as appropriate).
     *
     * This is to allow for developing the core gameplay of TFCR without worrying too much
     * about worldgen. Other biomes will slowly be mixed in as content is added.
     */
    private static <T extends IArea, C extends IContextExtended<T>> ImmutableList<IAreaFactory<T>> buildSimpleProcedure(long seed, WorldType worldTypeIn, OverworldGenSettings settings, LongFunction<C> contextFactory) {
        // Basic worldgen. In Vanilla this section handles plains/forest islands and oceans
        // iareafactory --> baseAreaFactory
        IAreaFactory<T> baseAreaFactory = GenLayerIsland.INSTANCE.apply(contextFactory.apply(1L));
        baseAreaFactory = GenLayerZoom.FUZZY.apply(contextFactory.apply(2000L), baseAreaFactory); // Zoom out 2x, fuzz
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(1L), baseAreaFactory); // Add small hill islands
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2001L), baseAreaFactory); // Zoom out 2x, normal
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory);  // Add 3 layers of small hill islands
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(50L), baseAreaFactory);
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(70L), baseAreaFactory);
        baseAreaFactory = GenLayerRemoveTooMuchOcean.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory); // If a region is all ocean, there's a 50% chance to flip it to flat.
        // Skip the ocean-related code, since we don't care about ocean temp yet
        // GenLayerAddSnow actually adds hilly + mountainous regions. So renamed to GenLayerAddMountain.
        baseAreaFactory = GenLayerAddMountain.INSTANCE.apply(contextFactory.apply(2L), baseAreaFactory);
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), baseAreaFactory); // More small hill islands
        // GenLayerEdge plays a role here. It looks like it messes with temperature (adding deserts/mountains),
        // and then adds a rare chance of having special placeholderBiomes. We don't care about that.
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2002L), baseAreaFactory); // Zoom out 2x
        baseAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(2003L), baseAreaFactory); // And again, for a total of 4x
        baseAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(4L), baseAreaFactory); // More small hill islands
        // Don't care about mushroom islands
        baseAreaFactory = GenLayerDeepOcean.INSTANCE.apply(contextFactory.apply(4L), baseAreaFactory); // Turn center ocean tiles into deep ocean

        // Custom. Tries to normalize land. 2 passes to potentially fully normalize mountain boundaries.
        baseAreaFactory = repeat(16L, GenLayerEqualize.INSTANCE, baseAreaFactory, 2, contextFactory);
        baseAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, baseAreaFactory, 0, contextFactory); // no-op?

        // River and biome size setup
        int biomeSize = 4;
        int riverSize = 4;
//        if (settings != null) {
//            biomeSize = settings.getBiomeSize();
//            riverSize = settings.getRiverSize();
//        }

        // lvt_7_1_ --> riverAreaFactory
        IAreaFactory<T> riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, baseAreaFactory, 0, contextFactory); // This is a no-op?
        riverAreaFactory = GenLayerRiverInit.INSTANCE.apply(contextFactory.apply(100L), riverAreaFactory);

        // Add Biomes using getBiomeLayer. The TFCR variation uses TerrainType-based logic.
        // lvt_8_1_ --> biomesAreaFactory
        IAreaFactory<T> biomesAreaFactory = worldTypeIn.getBiomeLayer(baseAreaFactory, settings, contextFactory);

        // lvt_9_1_ --> zoomedRiverInitAreaFactory
//        IAreaFactory<T> zoomedRiverInitAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, 2, contextFactory);
        biomesAreaFactory = GenLayerHills.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Random chance to raise land (modified from orig)
        riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, 2, contextFactory);
        riverAreaFactory = repeat(1000L, GenLayerZoom.NORMAL, riverAreaFactory, riverSize, contextFactory);
        riverAreaFactory = GenLayerRiver.INSTANCE.apply(contextFactory.apply(1L), riverAreaFactory); // Add rivers
        riverAreaFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), riverAreaFactory); // Smooth the region

        // Expand the placeholderBiomes as needed; based on biomeSize parameter
        for (int zoomIteration = 0; zoomIteration < biomeSize; zoomIteration++) {
            biomesAreaFactory = GenLayerZoom.NORMAL.apply(contextFactory.apply(1000L + zoomIteration), biomesAreaFactory);
            if (zoomIteration == 0) {
                biomesAreaFactory = GenLayerAddIsland.INSTANCE.apply(contextFactory.apply(3L), biomesAreaFactory); // Add 1 pass of small hill islands
            }

            if (zoomIteration == 1 || biomeSize == 1) {
                biomesAreaFactory = GenLayerShore.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Add beaches
            }
        }

        biomesAreaFactory = GenLayerSmooth.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory); // Smooth after zoom
        biomesAreaFactory = GenLayerRiverMask.INSTANCE.apply(contextFactory.apply(100L), biomesAreaFactory, riverAreaFactory); // Mix in the rivers

        // This lambda replaces GenLayerTempPrecip, which would normally fill the world with random temp/precip values.
        // "(0 << 8) | 35" maps to a temperature of 0 (+100 to move to positive byte range), precip of 35. This means
        // that we get a world that is filled with TemperateConiferousBiome biomes (and all its height variations).
        IAreaFactory<T> tempPrecipLayer = ((IAreaTransformer0) (context, areaDimensionIn, x, z) -> (100 << 8) | 35).apply(contextFactory.apply(17L));

        // Apply the temp/precip map on top.
        biomesAreaFactory = GenLayerTempPrecipMask.INSTANCE.apply(contextFactory.apply(1000L), biomesAreaFactory, tempPrecipLayer);

        // No ocean temperature mixing at this time
        IAreaFactory<T> voronoiZoomed = GenLayerVoronoiZoom.INSTANCE.apply(contextFactory.apply(10L), biomesAreaFactory);

        return ImmutableList.of(biomesAreaFactory, voronoiZoomed, biomesAreaFactory);
    }

    // Directly copied from LayerUtils
    public static GenLayer[] buildOverworldProcedure(long seed, WorldType typeIn, OverworldGenSettings settings) {
        int i = 1;
        int[] aint = new int[1];
        ImmutableList<IAreaFactory<LazyArea>> immutablelist = buildOverworldProcedure(seed, typeIn, settings, (p_202825_3_) -> {
            ++aint[0];
            return new LazyAreaLayerContext(1, aint[0], seed, p_202825_3_);
        });
        GenLayer genlayer = new GenLayer(immutablelist.get(0));
        GenLayer genlayer1 = new GenLayer(immutablelist.get(1));
        GenLayer genlayer2 = new GenLayer(immutablelist.get(2));
        return new GenLayer[]{genlayer, genlayer1, genlayer2};
    }

    // Similar to buildOverworldProcedure, but calls buildSimpleProcedure instead.
    public static GenLayer[] buildSimpleProcedure(long seed, WorldType typeIn, OverworldGenSettings settings) {
        int i = 1;
        int[] aint = new int[1];
        ImmutableList<IAreaFactory<LazyArea>> immutablelist = buildSimpleProcedure(seed, typeIn, settings, (p_202825_3_) -> {
            ++aint[0];
            return new LazyAreaLayerContext(1, aint[0], seed, p_202825_3_);
        });
        GenLayer genlayer = new GenLayer(immutablelist.get(0));
        GenLayer genlayer1 = new GenLayer(immutablelist.get(1));
        GenLayer genlayer2 = new GenLayer(immutablelist.get(2));
        return new GenLayer[]{genlayer, genlayer1, genlayer2};
    }
}