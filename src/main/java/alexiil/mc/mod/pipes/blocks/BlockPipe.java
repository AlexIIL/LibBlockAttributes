package alexiil.mc.mod.pipes.blocks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.VerticalEntityPosition;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateFactory.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.IAttributeBlock;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;

public abstract class BlockPipe extends Block implements BlockEntityProvider, IAttributeBlock, Waterloggable {

    public static final VoxelShape DEFAULT_PIPE_SHAPE = VoxelShapes.cube(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    public BlockPipe(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.with(Properties.WATERLOGGED);
    }

    @Override
    public boolean isTranslucent(BlockState state, BlockView view, BlockPos pos) {
        return !(Boolean) state.get(Properties.WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState blockState_1) {
        return blockState_1.get(Properties.WATERLOGGED) ? Fluids.WATER.getState(false)
            : super.getFluidState(blockState_1);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState blockState_1, Direction direction_1, BlockState blockState_2,
        IWorld iWorld_1, BlockPos blockPos_1, BlockPos blockPos_2) {
        if (blockState_1.get(Properties.WATERLOGGED)) {
            iWorld_1.getFluidTickScheduler().schedule(blockPos_1, Fluids.WATER, Fluids.WATER.getTickRate(iWorld_1));
        }

        return super.getStateForNeighborUpdate(blockState_1, direction_1, blockState_2, iWorld_1, blockPos_1,
            blockPos_2);
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState_1 = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return this.getDefaultState().with(Properties.WATERLOGGED,
            fluidState_1.matches(FluidTags.WATER) && fluidState_1.getLevel() == 8);
    }

    @Override
    public abstract TilePipe createBlockEntity(BlockView var1);

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos,
        VerticalEntityPosition entityPos) {
        BlockEntity be = view.getBlockEntity(pos);

        if (be instanceof TilePipe) {
            TilePipe pipe = (TilePipe) be;
            return pipe.getOutlineShape(entityPos);
        }

        return DEFAULT_PIPE_SHAPE;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos thisPos, Block neighbourBlock,
        BlockPos neighbourPos) {
        BlockEntity be = world.getBlockEntity(thisPos);
        if (be instanceof TilePipe) {
            TilePipe pipe = (TilePipe) be;
            pipe.setWorld(world);
            pipe.onNeighbourChange(neighbourBlock, neighbourPos);
        }
    }

    @Override
    public <T> void addAllAttributesFromDirection(World world, BlockPos pos, BlockState state, Attribute<T> attribute,
        List<T> resultingList, Direction searchDirection) {
        BlockEntity be = world.getBlockEntity(pos);
        if (this instanceof BlockPipeWooden) {
            if (attribute == IItemInsertable.ATTRIBUTE_INSERTABLE) {
                if (be instanceof TilePipe) {
                    int id = searchDirection.getOpposite().getId();
                    resultingList.add(attribute.cast(((TilePipe) be).insertables[id]));
                } else {
                    resultingList.add(attribute.cast(RejectingItemInsertable.EXTRACTOR));
                }
            }
        } else {
            if (attribute == IItemExtractable.ATTRIBUTE_EXTRACTABLE) {
                resultingList.add(attribute.cast(EmptyItemExtractable.SUPPLIER));
            }
        }
    }
}
