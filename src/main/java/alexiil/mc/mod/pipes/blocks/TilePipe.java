package alexiil.mc.mod.pipes.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.packet.BlockEntityUpdateS2CPacket;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.VerticalEntityPosition;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.IItemExtractable;
import alexiil.mc.lib.attributes.item.IItemInsertable;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.IStackFilter;
import alexiil.mc.lib.attributes.item.impl.RejectingItemInsertable;
import alexiil.mc.mod.pipes.util.DelayedList;
import alexiil.mc.mod.pipes.util.TagUtil;

public abstract class TilePipe extends BlockEntity implements Tickable, BlockEntityClientSerializable {

    protected static final double EXTRACT_SPEED = 0.08;

    private static final VoxelShape[] FACE_SHAPES;
    private static final VoxelShape[] SHAPES;

    static {
        FACE_SHAPES = new VoxelShape[6];
        for (Direction dir : Direction.values()) {
            double x = 0.5 + dir.getOffsetX() * 0.375;
            double y = 0.5 + dir.getOffsetY() * 0.375;
            double z = 0.5 + dir.getOffsetZ() * 0.375;
            double rx = dir.getAxis() == Axis.X ? 0.125 : 0.25;
            double ry = dir.getAxis() == Axis.Y ? 0.125 : 0.25;
            double rz = dir.getAxis() == Axis.Z ? 0.125 : 0.25;
            FACE_SHAPES[dir.ordinal()] = VoxelShapes.cube(x - rx, y - ry, z - rz, x + rx, y + ry, z + rz);
        }

        SHAPES = new VoxelShape[2 * 2 * 2 * 2 * 2 * 2];
        final VoxelShape base = BlockPipe.DEFAULT_PIPE_SHAPE;
        for (int c = 0; c < 0b111_111; c++) {
            VoxelShape shape = base;

            for (Direction dir : Direction.values()) {
                if ((c & (1 << dir.ordinal())) != 0) {
                    shape = VoxelShapes.combine(shape, FACE_SHAPES[dir.ordinal()], BooleanBiFunction.OR);
                }
            }

            SHAPES[c] = shape.simplify();
        }
    }

    public final BlockPipe pipeBlock;
    public volatile PipeBlockModelState blockModelState;
    private byte connections;
    final IItemInsertable[] insertables;

    private final DelayedList<TravellingItem> items = new DelayedList<>();

    public TilePipe(BlockEntityType<?> type, BlockPipe pipeBlock) {
        super(type);
        this.pipeBlock = pipeBlock;
        blockModelState = createModelState();
        insertables = new IItemInsertable[6];
        for (Direction dir : Direction.values()) {
            insertables[dir.ordinal()] = new IItemInsertable() {
                @Override
                public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {

                    return ItemStack.EMPTY;
                }

                @Override
                public IStackFilter getInsertionFilter() {
                    return IStackFilter.ANY_STACK;
                }
            };
        }
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        connections = tag.getByte("c");
        System.out.println("from_tag()  " + getPos() + " " + connections);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag = super.toTag(tag);
        tag.putByte("c", connections);
        return tag;
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        if (tag.getBoolean("is_item")) {

            // tag.put("item", item.stack.toTag(new CompoundTag()));
            // tag.putBoolean("to_center", item.toCenter);
            // tag.put("side", TagUtil.writeEnum(item.side));
            // tag.put("colour", TagUtil.writeEnum(item.colour));
            // tag.putShort("time", item.timeToDest > Short.MAX_VALUE ? Short.MAX_VALUE :(short) item.timeToDest);

            TravellingItem item = new TravellingItem(ItemStack.fromTag(tag.getCompound("item")));
            item.toCenter = tag.getBoolean("to_center");
            item.side = TagUtil.readEnum(tag.getTag("side"), Direction.class);
            item.colour = TagUtil.readEnum(tag.getTag("colour"), DyeColor.class);
            item.timeToDest = Short.toUnsignedInt(tag.getShort("time"));
            item.tickStarted = world.getTime() + 1;
            item.tickFinished = item.tickStarted + item.timeToDest;
            items.add(item.timeToDest + 1, item);

        } else {
            connections = tag.getByte("c");
            refreshModel();
        }
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        tag.putByte("c", connections);
        return tag;
    }

    @Override
    public CompoundTag toInitialChunkDataTag() {
        CompoundTag tag = super.toInitialChunkDataTag();
        tag.putByte("c", connections);
        return tag;
    }

    public VoxelShape getOutlineShape(VerticalEntityPosition entityPos) {
        if (connections == 0) {
            return BlockPipe.DEFAULT_PIPE_SHAPE;
        }
        return SHAPES[connections & 0b111111];
    }

    protected void onNeighbourChange(Block neighbourBlock, BlockPos neighbourPos) {
        for (Direction dir : Direction.values()) {
            BlockEntity oTile = world.getBlockEntity(getPos().offset(dir));
            if (oTile instanceof TilePipe || canConnect(dir, oTile) || (this instanceof TilePipeSided
                && ((TilePipeSided) this).currentDirection() == dir && ((TilePipeSided) this).canFaceDirection(dir))) {
                connect(dir);
            } else {
                disconnect(dir);
            }
        }
    }

    protected boolean canConnect(Direction dir, @Nullable BlockEntity oTile) {
        return getNeighbourInsertable(dir, oTile, true) != RejectingItemInsertable.NULL_INSERTABLE;
    }

    @Nullable
    public final TilePipe getNeighbourPipe(Direction dir) {
        World w = getWorld();
        if (w == null) {
            return null;
        }
        BlockEntity be = w.getBlockEntity(getPos().offset(dir));
        if (be instanceof TilePipe) {
            return (TilePipe) be;
        }
        return null;
    }

    @Nonnull
    public final IItemExtractable getNeighbourExtractable(Direction dir) {
        return getNeighbourExtractable(dir, null, false);
    }

    @Nonnull
    public final IItemExtractable getNeighbourExtractable(Direction dir, @Nullable BlockEntity entity,
        boolean entityIsKnownNull) {

        return ItemInvUtil.getExtractable(getWorld(), getPos().offset(dir), dir.getOpposite());
    }

    @Nonnull
    public final IItemInsertable getNeighbourInsertable(Direction dir) {
        return getNeighbourInsertable(dir, null, false);
    }

    @Nonnull
    public final IItemInsertable getNeighbourInsertable(Direction dir, @Nullable BlockEntity entity,
        boolean entityIsKnownNull) {

        return ItemInvUtil.getInsertable(getWorld(), getPos().offset(dir), dir.getOpposite());
    }

    protected PipeBlockModelState createModelState() {
        return new PipeBlockModelState(pipeBlock, encodeConnectedSides());
    }

    protected final byte encodeConnectedSides() {
        return connections;
    }

    public boolean isConnected(Direction dir) {
        return (connections & (1 << dir.ordinal())) != 0;
    }

    public void connect(Direction dir) {
        connections |= 1 << dir.ordinal();
        refreshModel();
    }

    public void disconnect(Direction dir) {
        connections &= ~(1 << dir.ordinal());
        refreshModel();
    }

    protected void refreshModel() {
        blockModelState = createModelState();
        World w = getWorld();
        if (w instanceof ServerWorld) {
            // method_18766 = getPlayers()
            sendPacket((ServerWorld) w, this.toUpdatePacket());
        } else if (w != null) {
            w.scheduleBlockRender(getPos());
        }
    }

    private void sendPacket(ServerWorld w, BlockEntityUpdateS2CPacket packet) {
        w.method_18766(player -> player.squaredDistanceTo(getPos()) < 24 * 24)
            .forEach(player -> player.networkHandler.sendPacket(packet));
    }

    public static class PipeBlockModelState {
        public final BlockPipe block;
        final byte connections;

        public PipeBlockModelState(BlockPipe block, byte isConnected) {
            this.block = block;
            this.connections = isConnected;
        }

        public boolean isConnected(Direction dir) {
            return (connections & (1 << dir.ordinal())) != 0;
        }

        @Override
        public String toString() {
            return "PipeBlockModel{" + block + ", " + connections + "}";
        }
    }

    public double getPipeLength(Direction side) {
        if (side == null) {
            return 0;
        }
        if (isConnected(side)) {
            if (getNeighbourPipe(side) == null/* pipe.getConnectedType(side) == ConnectedType.TILE */) {
                // TODO: Check the length between this pipes centre and the next block along
                return 0.5 + 0.25;// Tiny distance for fully pushing items in.
            }
            return 0.5;
        } else {
            return 0.25;
        }
    }

    @Override
    public void tick() {

        List<TravellingItem> toTick = items.advance();
        long currentTime = world.getTime();

        for (TravellingItem item : toTick) {
            if (item.tickFinished > currentTime) {
                // Can happen if something ticks this tile multiple times in a single real tick
                items.add((int) (item.tickFinished - currentTime), item);
                continue;
            }
            if (item.isPhantom) {
                continue;
            }
            if (world.isClient) {
                // TODO: Client item advancing/intelligent stuffs

                continue;
            }
            if (item.toCenter) {
                onItemReachCenter(item);
            } else {
                onItemReachEnd(item);
            }
        }
    }

    void sendItemDataToClient(TravellingItem item) {
        // TODO :p
        // System.out.println(getPos() + " - " + item.stack + " - " + item.side);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BlockEntityType.getId(getType()).toString());
        tag.putBoolean("is_item", true);

        tag.put("item", item.stack.toTag(new CompoundTag()));
        tag.putBoolean("to_center", item.toCenter);
        tag.put("side", TagUtil.writeEnum(item.side));
        tag.put("colour", TagUtil.writeEnum(item.colour));
        tag.putShort("time", item.timeToDest > Short.MAX_VALUE ? Short.MAX_VALUE : (short) item.timeToDest);

        sendPacket((ServerWorld) world, new BlockEntityUpdateS2CPacket(getPos(), 127, tag));
    }

    protected List<EnumSet<Direction>> getOrderForItem(TravellingItem item, EnumSet<Direction> validDirections) {
        List<EnumSet<Direction>> list = new ArrayList<>();

        if (!validDirections.isEmpty()) {
            list.add(validDirections);
        }

        return list;
    }

    protected boolean canBounce() {
        return false;
    }

    private void onItemReachCenter(TravellingItem item) {

        if (item.stack.isEmpty()) {
            return;
        }

        EnumSet<Direction> dirs = EnumSet.allOf(Direction.class);
        dirs.remove(item.side);
        dirs.removeAll(item.tried);
        for (Direction dir : Direction.values()) {
            if (!isConnected(dir) || getNeighbourInsertable(dir) == null) {
                dirs.remove(dir);
            }
        }

        List<EnumSet<Direction>> order = getOrderForItem(item, dirs);
        if (order.isEmpty()) {
            if (canBounce()) {
                order = ImmutableList.of(EnumSet.of(item.side));
            } else {
                dropItem(item.stack, null, item.side.getOpposite(), item.speed);
                return;
            }
        }

        long now = world.getTime();
        // Saves effort :p
        final double newSpeed = 0.08;
        //
        // if (holder.fireEvent(modifySpeed)) {
        // double target = modifySpeed.targetSpeed;
        // double maxDelta = modifySpeed.maxSpeedChange;
        // if (item.speed < target) {
        // newSpeed = Math.min(target, item.speed + maxDelta);
        // } else if (item.speed > target) {
        // newSpeed = Math.max(target, item.speed - maxDelta);
        // } else {
        // newSpeed = item.speed;
        // }
        // } else {
        // // Nothing affected the speed
        // // so just fallback to a sensible default
        // if (item.speed > 0.03) {
        // newSpeed = Math.max(0.03, item.speed - PipeBehaviourStone.SPEED_DELTA);
        // } else {
        // newSpeed = item.speed;
        // }
        // }

        List<Direction> destinations = new ArrayList<>();

        for (EnumSet<Direction> set : order) {
            List<Direction> shuffled = new ArrayList<>();
            shuffled.addAll(set);
            Collections.shuffle(shuffled);
            destinations.addAll(shuffled);
        }

        if (destinations.size() == 0) {
            dropItem(item.stack, null, item.side.getOpposite(), newSpeed);
        } else {
            TravellingItem newItem = new TravellingItem(item.stack);
            newItem.tried.addAll(item.tried);
            newItem.toCenter = false;
            newItem.colour = item.colour;
            newItem.side = destinations.get(0);
            newItem.speed = newSpeed;
            newItem.genTimings(now, getPipeLength(newItem.side));
            items.add(newItem.timeToDest, newItem);
            sendItemDataToClient(newItem);
        }
    }

    private void onItemReachEnd(TravellingItem item) {
        IItemInsertable ins = getNeighbourInsertable(item.side);
        ItemStack excess = item.stack;
        if (ins != null) {
            Direction oppositeSide = item.side.getOpposite();
            TilePipe oPipe = getNeighbourPipe(item.side);

            if (oPipe != null) {
                excess = oPipe.injectItem(excess, true, oppositeSide, item.colour, item.speed);
            } else {
                excess = ins.attemptInsertion(excess, Simulation.ACTION);
            }
        }
        if (excess.isEmpty()) {
            return;
        }
        item.tried.add(item.side);
        item.toCenter = true;
        item.stack = excess;
        item.genTimings(world.getTime(), getPipeLength(item.side));
        items.add(item.timeToDest, item);
        sendItemDataToClient(item);
    }

    private void dropItem(ItemStack stack, Direction side, Direction motion, double speed) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        double x = pos.getX() + 0.5 + motion.getOffsetX() * 0.5;
        double y = pos.getY() + 0.5 + motion.getOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + motion.getOffsetZ() * 0.5;
        speed += 0.01;
        speed *= 2;
        ItemEntity ent = new ItemEntity(world, x, y, z, stack);
        ent.setVelocity(new Vec3d(motion.getVector()).multiply(speed));

        world.spawnEntity(ent);
    }

    public boolean canInjectItems(Direction from) {
        return isConnected(from);
    }

    public ItemStack injectItem(@Nonnull ItemStack stack, boolean doAdd, Direction from, DyeColor colour,
        double speed) {
        if (world.isClient) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (!canInjectItems(from)) {
            return stack;
        }

        if (speed < 0.01) {
            speed = 0.01;
        }

        // Try insert

        ItemStack toSplit = ItemStack.EMPTY;
        ItemStack toInsert = stack;

        if (doAdd) {
            insertItemEvents(toInsert, colour, speed, from);
        }

        if (toSplit.isEmpty()) {
            toSplit = ItemStack.EMPTY;
        }

        return toSplit;
    }

    public void insertItemsForce(@Nonnull ItemStack stack, Direction from, DyeColor colour, double speed) {
        if (world.isClient) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (stack.isEmpty()) {
            return;
        }
        if (speed < 0.01) {
            speed = 0.01;
        }
        long now = world.getTime();
        TravellingItem item = new TravellingItem(stack);
        item.side = from;
        item.toCenter = true;
        item.speed = speed;
        item.colour = colour;
        item.genTimings(now, 0);
        item.tried.add(from);
        addItemTryMerge(item);
    }

    /** Used internally to split up manual insertions from controlled extractions. */
    private void insertItemEvents(@Nonnull ItemStack toInsert, DyeColor colour, double speed, Direction from) {
        long now = world.getTime();

        TravellingItem item = new TravellingItem(toInsert);
        item.side = from;
        item.toCenter = true;
        item.speed = speed;
        item.colour = colour;
        item.stack = toInsert;
        item.genTimings(now, getPipeLength(from));
        item.tried.add(from);
        addItemTryMerge(item);
    }

    private void addItemTryMerge(TravellingItem item) {
        // for (List<TravellingItem> list : items.getAllElements()) {
        // for (TravellingItem item2 : list) {
        // if (item2.mergeWith(item)) {
        // return;
        // }
        // }
        // }
        items.add(item.timeToDest, item);
        sendItemDataToClient(item);
    }

    @Nullable
    private static EnumSet<Direction> getFirstNonEmptySet(List<EnumSet<Direction>> possible) {
        for (EnumSet<Direction> set : possible) {
            if (set.size() > 0) {
                return set;
            }
        }
        return null;
    }

    public List<TravellingItem> getAllItemsForRender() {
        List<TravellingItem> all = new ArrayList<>();
        for (List<TravellingItem> innerList : items.getAllElements()) {
            all.addAll(innerList);
        }
        return all;
    }
}
