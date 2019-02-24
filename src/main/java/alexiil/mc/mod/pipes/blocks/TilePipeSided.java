package alexiil.mc.mod.pipes.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;

public abstract class TilePipeSided extends TilePipe {

    private Direction currentDirection = null;

    public TilePipeSided(BlockEntityType<?> type, BlockPipe pipeBlock) {
        super(type, pipeBlock);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag = super.toTag(tag);
        tag.putByte("dir", (byte) (currentDirection == null ? 0xFF : currentDirection.getId()));
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        byte b = tag.getByte("dir");
        if (b >= 0 && b < 6) {
            currentDirection = Direction.byId(b);
        } else {
            currentDirection = null;
        }
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        tag = super.toClientTag(tag);
        tag.putByte("dir", (byte) (currentDirection == null ? 0xFF : currentDirection.getId()));
        return tag;
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        super.fromClientTag(tag);
        if (!tag.getBoolean("is_item")) {
            byte b = tag.getByte("dir");
            if (b >= 0 && b < 6) {
                currentDirection = Direction.byId(b);
            } else {
                currentDirection = null;
            }
            refreshModel();
        }
    }

    @Override
    public CompoundTag toInitialChunkDataTag() {
        CompoundTag tag = super.toInitialChunkDataTag();
        tag.putByte("dir", (byte) (currentDirection == null ? 0xFF : currentDirection.getId()));
        return tag;
    }

    public Direction currentDirection() {
        return currentDirection;
    }

    public void currentDirection(Direction dir) {
        if (currentDirection != dir && canFaceDirection(dir)) {
            this.currentDirection = dir;
            refreshModel();
        }
    }

    protected abstract boolean canFaceDirection(Direction dir);

    public boolean attemptRotation() {
        List<Direction> dirs = new ArrayList<>();
        Collections.addAll(dirs, Direction.values());

        Direction old = currentDirection;

        if (old != null) {
            int idx = old.getId();
            if (idx < 5) {
                dirs.addAll(dirs.subList(0, idx + 1));
                dirs.subList(0, idx + 1).clear();
            }
        }

        boolean connectedToAny = false;
        for (Direction dir : dirs) {
            if (canFaceDirection(dir) && !connectedToAny) {
                if (!isConnected(dir)) {
                    connect(dir);
                }
                currentDirection = dir;
                connectedToAny = true;
            } else if (isConnected(dir)) {
                BlockEntity oTile = world.getBlockEntity(getPos().offset(dir));
                if (!(oTile instanceof TilePipe) && !canConnect(dir, oTile)) {
                    disconnect(dir);
                }
            }
        }

        if (!connectedToAny) {
            currentDirection = null;
        }

        if (currentDirection != old) {
            refreshModel();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected PipeBlockModelStateSided createModelState() {
        return new PipeBlockModelStateSided(pipeBlock, encodeConnectedSides(), currentDirection);
    }

    public static class PipeBlockModelStateSided extends PipeBlockModelState {
        @Nullable
        public final Direction mainSide;

        public PipeBlockModelStateSided(BlockPipe block, byte isConnected, Direction mainSide) {
            super(block, isConnected);
            this.mainSide = mainSide;
        }
    }

}
