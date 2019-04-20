package alexiil.mc.lib.attributes.fluid;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.CacheInfo;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.CombinedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.EmptyFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.GroupedFluidInvViewFixedWrapper;
import alexiil.mc.lib.attributes.fluid.impl.MappedFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.impl.SubFixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A view of a fixed inventory for fluids, where the number of tanks never changes, and every tank is "simple":
 * <ul>
 * <li>The amount of every tank will never exceed 64, the normal maximum stack size of minecraft.</li>
 * <li>The stack will stay in the tank until it is removed or changed by something else. (So setting the stack in a tank
 * of an {@link FixedFluidInv} will reflect that change in {@link #getInvFluid(int)}).</li>
 * </ul>
 * <p>
 * The attribute is stored in {@link FluidAttributes#FIXED_INV_VIEW}.
 * <p>
 * There are various classes of interest:
 * <ul>
 * <li>A modifiable version of this is {@link FixedFluidInv}.</li>
 * <li>The null instance is {@link EmptyFixedFluidInv}</li>
 * <li>A combined view of several sub-inventories is {@link CombinedFixedFluidInvView}.</li>
 * <li>A partial view of a single inventory is {@link SubFixedFluidInv}</li>
 * </ul>
 */
public interface FixedFluidInvView {

    /** @return The number of tanks in this inventory. */
    int getTankCount();

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The FluidVolume that is held in the tank at the moment. The returned volume must never be modified!
     * @throws RuntimeException if the given tank wasn't a valid index. */
    FluidVolume getInvFluid(int tank);

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return The maximum amount that the given tank can hold of the given fluid. This method will ignore the current
     *         stack in {@link #getInvFluid(int)}. Note that any setters that this object implements (like
     *         {@link FixedFluidInv#setInvFluid(int, FluidVolume, Simulation)} should reject stacks that are greater
     *         than this value. (and callers should only call this if they need to check the amounts separately. Note
     *         that it is meaningless to return values greater than the maximum amount an fluid can be stacked to here,
     *         and callers are free to throw an exception if this is violated. (Basically huge single-tank inventories
     *         shouldn't implement this interface).
     * @throws RuntimeException if the given tank wasn't a valid index. */
    int getMaxAmount(int tank);

    /** Checks to see if the given stack is valid for a given tank. This ignores any current stacks in the tank.
     * 
     * @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @throws RuntimeException if the given tank wasn't a valid index. */
    boolean isFluidValidForTank(int tank, FluidKey fluid);

    /** @param tank The tank index. Must be a value between 0 (inclusive) and {@link #getTankCount()} (exclusive) to be
     *            valid. (Like in arrays, lists, etc).
     * @return An {@link FluidFilter} for this tank. If this tank is filtered by an {@link FluidFilter} internally then
     *         it is highly recommended that this be overridden to return *that* filter rather than a newly constructed
     *         one.
     * @throws RuntimeException if the given tank wasn't a valid index. */
    default FluidFilter getFilterForTank(int tank) {
        return stack -> isFluidValidForTank(tank, stack);
    }

    default SingleFluidTankView getTank(int tank) {
        return new SingleFluidTankView(this, tank);
    }

    /** @return A statistical view of this inventory. */
    default GroupedFluidInvView getGroupedInv() {
        return new GroupedFluidInvViewFixedWrapper(this);
    }

    /** Adds the given listener to this inventory, such that the
     * {@link FluidInvTankChangeListener#onChange(FixedFluidInvView, int, FluidVolume, FluidVolume)} will be called
     * every time that this inventory changes. However if this inventory doesn't support listeners then this will return
     * a null {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken);

    /** Equivalent to {@link List#subList(int, int)}.
     * 
     * @param fromIndex The first tank to expose
     * @param toIndex The tank after the last tank to expose.
     * @return a view of this inventory that only exposes the given number of tanks.
     * @throws RuntimeException if any of the given tanks weren't valid. */
    default FixedFluidInvView getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new SubFixedFluidInvView(this, fromIndex, toIndex);
    }

    /** @param tanks The tanks to expose.
     * @return a view of this inventory that only exposes the given number of tanks.
     * @throws RuntimeException if any of the given tanks weren't valid */
    default FixedFluidInvView getMappedInv(int... tanks) {
        if (tanks.length == 0) {
            return EmptyFixedFluidInv.INSTANCE;
        }
        return new MappedFixedFluidInvView(this, tanks);
    }

    /** Offers this object and {@link #getGroupedInv()} to the attribute list. (Which, in turn, adds
     * {@link FixedFluidInv#getInsertable()}, {@link FixedFluidInv#getExtractable()}, and
     * {@link FixedFluidInv#getTransferable()} to the list as well). */
    default void offerSelfAsAttribute(AttributeList<?> list, @Nullable CacheInfo cacheInfo,
        @Nullable VoxelShape shape) {
        list.offer(this, cacheInfo, shape);
        list.offer(getGroupedInv(), cacheInfo, shape);
    }

    /** @return An object that only implements {@link FixedFluidInvView}, and does not expose the modification methods
     *         that {@link FixedFluidInv} does. Implementations that don't expose any modification methods themselves
     *         should override this method to just return themselves. */
    default FixedFluidInvView getView() {
        final FixedFluidInvView real = this;
        return new FixedFluidInvView() {
            @Override
            public int getTankCount() {
                return real.getTankCount();
            }

            @Override
            public FluidVolume getInvFluid(int tank) {
                return real.getInvFluid(tank);
            }

            @Override
            public boolean isFluidValidForTank(int tank, FluidKey fluid) {
                return real.isFluidValidForTank(tank, fluid);
            }

            @Override
            public int getMaxAmount(int tank) {
                return real.getMaxAmount(tank);
            }

            @Override
            public FluidFilter getFilterForTank(int tank) {
                return real.getFilterForTank(tank);
            }

            @Override
            public GroupedFluidInvView getGroupedInv() {
                return new GroupedFluidInvViewFixedWrapper(this);
            }

            @Override
            public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
                final FixedFluidInvView view = this;
                return real.addListener((inv, tank, prev, curr) -> {
                    // Defend against giving the listener the real (possibly changeable) inventory.
                    // In addition the listener would probably cache *this view* rather than the backing inventory
                    // so they most likely need it to be this inventory.
                    listener.onChange(view, tank, prev, curr);
                }, removalToken);
            }
        };
    }
}
