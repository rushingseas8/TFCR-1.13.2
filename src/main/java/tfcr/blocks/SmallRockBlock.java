package tfcr.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.storage.loot.LootContext;
import tfcr.init.ModItems;
import tfcr.items.TFCRItem;

import java.util.ArrayList;
import java.util.List;

public class SmallRockBlock extends TFCRBlock {

    public SmallRockBlock(Properties properties, String name) {
        super(properties, name);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return Block.makeCuboidShape(8 - 3, 0, 8 - 3, 8 + 3, 4, 8 + 3);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return Block.makeCuboidShape(8 - 3, 0, 8 - 3, 8 + 3, 4, 8 + 3);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(ModItems.small_rock, 10));
        drops.add(new ItemStack(ModItems.leaves, 10));
        return drops;
    }
}
