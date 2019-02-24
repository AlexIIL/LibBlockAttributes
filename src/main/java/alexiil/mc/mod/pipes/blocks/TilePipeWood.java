package alexiil.mc.mod.pipes.blocks;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;

public class TilePipeWood extends TilePipeSided {

    public TilePipeWood() {
        super(SimplePipeBlocks.WOODEN_PIPE_TILE, SimplePipeBlocks.WOODEN_PIPE);
    }

    @Override
    protected boolean canConnect(Direction dir, BlockEntity oTile) {
        return false;
    }

    @Override
    protected boolean canFaceDirection(Direction dir) {
        return getNeighbourPipe(dir) == null && getNeighbourExtractable(dir) != EmptyItemExtractable.NULL_EXTRACTABLE;
    }

    @Override
    public void tick() {
        super.tick();
        if (world.isClient) {
            return;
        }
        Direction dir = currentDirection();
        if (dir == null) {
            return;
        }

        if (world.getReceivedRedstonePower(getPos()) > 0) {
            IItemExtractable extractable = getNeighbourExtractable(dir);
            if (extractable == null) {
                return;
            }

            ItemStack stack = extractable.attemptAnyExtraction(1, Simulation.ACTION);

            if (!stack.isEmpty()) {
                insertItemsForce(stack, dir, null, EXTRACT_SPEED);
            }
        }
    }
}
