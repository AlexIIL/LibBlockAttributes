/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package alexiil.mc.mod.pipes.blocks;

import java.util.EnumSet;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.mod.pipes.util.TagUtil;

public class TravellingItem {
    // Client fields - public for rendering
    // @Nonnull
    // public final Supplier<ItemStack> clientItemLink; -- optimisation not ported
//    public int stackSize;
    public DyeColor colour;

    // Server fields
    /** The server itemstack */
    @Nonnull
    public ItemStack stack;
    int id = 0;
    boolean toCenter;
    double speed = 0.05;
    /** Absolute times (relative to world.getTotalWorldTime()) with when an item started to when it finishes. */
    long tickStarted, tickFinished;
    /** Relative times (from tickStarted) until an event needs to be fired or this item needs changing. */
    int timeToDest;
    /** If {@link #toCenter} is true then this represents the side that the item is coming from, otherwise this
     * represents the side that the item is going to. */
    Direction side;
    /** A set of all the faces that this item has tried to go and failed. */
    EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);
    /** If true then events won't be fired for this, and this item won't be dropped by the pipe. However it will affect
     * pipe.isEmpty and related gate triggers. */
    boolean isPhantom = false;

    // @formatter:off
    /* States (server side):
      
      - TO_CENTER:
        - tickStarted is the tick that the item entered the pipe (or bounced back)
        - tickFinished is the tick that the item will reach the center 
        - side is the side that the item came from
        - timeToDest is equal to timeFinished - timeStarted
      
      - TO_EXIT:
       - tickStarted is the tick that the item reached the center
       - tickFinished is the tick that the item will reach the end of a pipe 
       - side is the side that the item is going to 
       - timeToDest is equal to timeFinished - timeStarted. 
     */
    // @formatter:on

    public TravellingItem(@Nonnull ItemStack stack) {
        this.stack = stack;
    }

//    public TravellingItem(ItemStack clientStack, int count) {
//        this.stackSize = count;
//        this.stack = clientStack;
//    }

    public TravellingItem(CompoundTag nbt, long tickNow) {
        stack = ItemStack.fromTag(nbt.getCompound("stack"));
        int c = nbt.getByte("colour");
        this.colour = c == 0 ? null : DyeColor.byId(c - 1);
        this.toCenter = nbt.getBoolean("toCenter");
        this.speed = nbt.getDouble("speed");
        if (speed < 0.001) {
            // Just to make sure that we don't have an invalid speed
            speed = 0.001;
        }
        tickStarted = nbt.getInt("tickStarted") + tickNow;
        tickFinished = nbt.getInt("tickFinished") + tickNow;
        timeToDest = nbt.getInt("timeToDest");

        side = TagUtil.readEnum(nbt.getTag("side"), Direction.class);
        if (side == null || timeToDest == 0) {
            // Older 8.0.x. version
            toCenter = true;
        }
        tried = TagUtil.readEnumSet(nbt.getTag("tried"), Direction.class);
        isPhantom = nbt.getBoolean("isPhantom");
    }

    public CompoundTag writeToNbt(long tickNow) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("stack", stack.toTag(new CompoundTag()));
        nbt.putByte("colour", (byte) (colour == null ? 0 : colour.getId() + 1));
        nbt.putBoolean("toCenter", toCenter);
        nbt.putDouble("speed", speed);
        nbt.putInt("tickStarted", (int) (tickStarted - tickNow));
        nbt.putInt("tickFinished", (int) (tickFinished - tickNow));
        nbt.putInt("timeToDest", timeToDest);
        nbt.put("side", TagUtil.writeEnum(side));
        nbt.put("tried", TagUtil.writeEnumSet(tried, Direction.class));
        if (isPhantom) {
            nbt.putBoolean("isPhantom", true);
        }
        return nbt;
    }

    public int getCurrentDelay(long tickNow) {
        long diff = tickFinished - tickNow;
        if (diff < 0) {
            return 0;
        } else {
            return (int) diff;
        }
    }

    public double getWayThrough(long now) {
        long diff = tickFinished - tickStarted;
        long nowDiff = now - tickStarted;
        return nowDiff / (double) diff;
    }

    public void genTimings(long now, double distance) {
        tickStarted = now;
        timeToDest = (int) Math.ceil(distance / speed);
        tickFinished = now + timeToDest;
    }

    public boolean canMerge(TravellingItem with) {
        if (isPhantom || with.isPhantom) {
            return false;
        }
        return toCenter == with.toCenter//
            && colour == with.colour//
            && side == with.side//
            && Math.abs(tickFinished - with.tickFinished) < 4//
            && stack.getMaxAmount() >= stack.getAmount() + with.stack.getAmount()//
            && ItemStackUtil.areEqualIgnoreAmounts(stack, with.stack);
    }

    /** Attempts to merge the two travelling item's together, if they are close enough.
     * 
     * @param with
     * @return */
    public boolean mergeWith(TravellingItem with) {
        if (canMerge(with)) {
            this.stack.addAmount(with.stack.getAmount());
            return true;
        }
        return false;
    }

    public Vec3d interpolatePosition(Vec3d start, Vec3d end, long tick, float partialTicks) {
        long diff = tickFinished - tickStarted;
        long nowDiff = tick - tickStarted;
        double sinceStart = nowDiff + partialTicks;
        double interpMul = sinceStart / diff;
        double oneMinus = 1 - interpMul;
        if (interpMul <= 0) return start;
        if (interpMul >= 1) return end;

        double x = oneMinus * start.x + interpMul * end.x;
        double y = oneMinus * start.y + interpMul * end.y;
        double z = oneMinus * start.z + interpMul * end.z;
        return new Vec3d(x, y, z);
    }

    public Vec3d getRenderPosition(BlockPos pos, long tick, float partialTicks, TilePipe pipe) {
        long diff = tickFinished - tickStarted;
        long afterTick = tick - tickStarted;

        float interp = (afterTick + partialTicks) / diff;
        interp = Math.max(0, Math.min(1, interp));

        Vec3d center = new Vec3d(pos).add(0.5, 0.5, 0.5);
        Vec3d vecSide =
            side == null ? center : center.add(new Vec3d(side.getVector()).multiply(pipe.getPipeLength(side)));

        Vec3d vecFrom;
        Vec3d vecTo;
        if (toCenter) {
            vecFrom = vecSide;
            vecTo = center;
        } else {
            vecFrom = center;
            vecTo = vecSide;
        }

        return vecFrom.multiply(1 - interp).add(vecTo.multiply(interp));
    }

    public Direction getRenderDirection(long tick, float partialTicks) {
        long diff = tickFinished - tickStarted;
        long afterTick = tick - tickStarted;

        float interp = (afterTick + partialTicks) / diff;
        interp = Math.max(0, Math.min(1, interp));
        if (toCenter) {
            return side == null ? null : side.getOpposite();
        } else {
            return side;
        }
    }

    public boolean isVisible() {
        return true;
    }
}
