/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.AttributeSourceType;
import alexiil.mc.lib.attributes.CompatLeveledMap;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidContainerRegistry.FluidFillHandler.StackReturnFunc;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.AbstractItemBasedAttribute;
import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.LimitedConsumer;
import alexiil.mc.lib.attributes.misc.Reference;
import alexiil.mc.lib.attributes.misc.StackReference;

/** Maps {@link Item} instances to empty and full containers. This also handles registering more complex behaviour: for
 * example a custom handler to fill a glass bottle with potions. */
public final class FluidContainerRegistry {
    private FluidContainerRegistry() {}

    // TODO: Expose mapContainer via LibModMail
    // (We can't really expose registerFillHandler usefully, as it's heavily dependent on classes)

    private static final Map<Item, ItemContainerState> containerStates = new IdentityHashMap<>();
    private static final Map<FluidKey, Set<Item>> fullContainers = new HashMap<>();
    private static final Set<Item> emptyItems = new HashSet<>();
    private static final Set<Item> fullItems = new HashSet<>();

    private static final Map<FluidKey, Set<Item>> fullContainersUn = new HashMap<>();
    private static final Set<Item> emptyItemsUn = Collections.unmodifiableSet(emptyItems);
    private static final Set<Item> fullItemsUn = Collections.unmodifiableSet(fullItems);

    // #############
    //
    // Registration
    //
    // #############

    /** Directly maps a given {@link Item} as containing the given fluid, with another {@link Item} as the empty
     * container. This is the most simple mapping possible: no NBT is used to store the fluid data, and is
     * bi-directional: you can drain the fluid from the full item to get both the given fluid (and the empty item), and
     * fill the empty item with the same amount of the given fluid to get the full item. */
    public static void mapContainer(Item empty, Item full, FluidVolume fluid) {
        ItemContainerState forEmpty = containerStates.get(empty);

        boolean putEmpty = false;
        if (forEmpty == null) {
            forEmpty = new StateEmpty(empty);
            putEmpty = true;
        }

        if (forEmpty instanceof StateFull) {
            throw new IllegalArgumentException(
                "The item " + forEmpty.itemToString()
                    + " is already mapped as a full item - so we cannot remap it as empty to allow it to be filled with "
                    + fluid + " to make " + ItemContainerState.itemToString(full)
            );
        }

        ((StateEmpty) forEmpty).mapDirectContainer(full, fluid);

        if (putEmpty) {
            containerStates.put(empty, forEmpty);
            forEmpty.addItemAttributes();
        }
        fullContainers.computeIfAbsent(fluid.fluidKey, k -> new HashSet<>()).add(full);
        emptyItems.add(empty);
        fullItems.add(full);
    }

    /** Registers a custom fill handler for the given empty item, which always creates filled {@link ItemStack}s with
     * <em>different</em> {@link Item}s to the original. (In other words, this is single-directional: the filled item
     * must expose {@link GroupedFluidInv} separately in order to be drained back to the empty {@link Item}). */
    public static void registerFillHandler(Item empty, FluidKey fluid, FluidFillHandler handler) {
        registerFillHandlerInternal(empty, handler, state -> {
            state.variants.putExact(AttributeSourceType.INSTANCE, fluid, handler);
        });
    }

    /** Registers a custom fill handler for the given empty item, which always creates filled {@link ItemStack}s with
     * <em>different</em> {@link Item}s to the original. (In other words, this is single-directional: the filled item
     * must expose {@link GroupedFluidInv} separately in order to be drained back to the empty {@link Item}). */
    public static void registerFillHandler(
        Item empty, FluidKey fluid, FluidAmount minimum, FluidAmount capacity, FluidFillFunction fn
    ) {
        registerFillHandler(empty, fluid, new FluidFillHandlerWrapper(minimum, capacity, fn));
    }

    /** Registers a custom fill handler for the given empty item, which always creates filled {@link ItemStack}s with
     * <em>different</em> {@link Item}s to the original. (In other words, this is single-directional: the filled item
     * must expose {@link GroupedFluidInv} separately in order to be drained back to the empty {@link Item}).
     * 
     * @param empty The empty item to fill from
     * @param fluids The filter to test which fluids can be filled using this handler.
     * @param isFilterSpecific If true then this will take priority over {@link Class} based mappings.
     * @param handler */
    public static void registerFillHandler(
        Item empty, FluidFilter fluids, boolean isFilterSpecific, FluidFillHandler handler
    ) {
        registerFillHandlerInternal(empty, handler, state -> {
            state.variants
                .addPredicateBased(AttributeSourceType.INSTANCE, isFilterSpecific, fluids.asPredicate(), handler);
        });
    }

    public static void registerFillHandler(
        Item empty, FluidFilter fluids, boolean isFilterSpecific, FluidAmount minimum, FluidAmount capacity,
        FluidFillFunction fn
    ) {
        registerFillHandler(empty, fluids, isFilterSpecific, new FluidFillHandlerWrapper(minimum, capacity, fn));
    }

    /** Registers a custom fill handler for the given empty item, which always creates filled {@link ItemStack}s with
     * <em>different</em> {@link Item}s to the original. (In other words, this is single-directional: the filled item
     * must expose {@link GroupedFluidInv} separately in order to be drained back to the empty {@link Item}).
     * <p>
     * This variant applies to <em>every</em> {@link FluidKey}.
     * 
     * @param empty
     * @param handler */
    public static void registerFillHandler(Item empty, FluidFillHandler handler) {
        registerFillHandler(empty, ConstantFluidFilter.ANYTHING, false, handler);
    }

    public static void registerFillHandler(
        Item empty, FluidAmount minimum, FluidAmount capacity, FluidFillFunction fn
    ) {
        registerFillHandler(empty, ConstantFluidFilter.ANYTHING, false, minimum, capacity, fn);
    }

    /** Registers a custom fill handler for the given empty item, which always creates filled {@link ItemStack}s with
     * <em>different</em> {@link Item}s to the original. (In other words, this is single-directional: the filled item
     * must expose {@link GroupedFluidInv} separately in order to be drained back to the empty {@link Item}).
     * 
     * @param empty The empty item to fill from
     * @param keyClass The base class to test against.
     * @param matchSubclasses If true then subclasses of the given {@link Class} will also be matched.
     * @param handler */
    public static void registerFillHandler(
        Item empty, Class<?> keyClass, boolean matchSubclasses, FluidFillHandler handler
    ) {
        registerFillHandlerInternal(empty, handler, state -> {
            state.variants.putClassBased(AttributeSourceType.INSTANCE, keyClass, matchSubclasses, handler);
        });
    }

    private static void registerFillHandlerInternal(
        Item empty, FluidFillHandler handler, Consumer<StateEmpty> registor
    ) {
        ItemContainerState forEmpty = containerStates.get(empty);

        boolean putEmpty = false;
        if (forEmpty == null) {
            forEmpty = new StateEmpty(empty);
            putEmpty = true;
        }

        if (forEmpty instanceof StateFull) {
            throw new IllegalArgumentException(
                "The item " + forEmpty.itemToString()
                    + " is already mapped as a full item - so we cannot remap it as empty to allow it to be handled by "
                    + handler
            );
        }

        StateEmpty state = ((StateEmpty) forEmpty);
        registor.accept(state);
        state.minimum = state.minimum == null ? handler.minimum : state.minimum.min(handler.minimum);

        if (putEmpty) {
            containerStates.put(empty, forEmpty);
            forEmpty.addItemAttributes();
        }
        emptyItems.add(empty);
    }

    // #########
    //
    // Getters
    //
    // #########

    /** Retrieves every {@link Item} that has been registered with {@link #mapContainer(Item, Item, FluidVolume)} as a
     * direct container that is full of the specified fluid.
     * 
     * @return An unmodifiable view of the set of {@link Item}s which contain the given {@link FluidKey}. (Which also
     *         updates as new entries are registered). */
    public static Set<Item> getFullContainersFor(FluidKey fluid) {
        Set<Item> set = fullContainersUn.get(fluid);
        if (set == null) {
            Set<Item> internal = fullContainers.get(fluid);
            if (internal == null) {
                internal = new HashSet<>();
                fullContainers.put(fluid, internal);
            }
            set = Collections.unmodifiableSet(internal);
            fullContainersUn.put(fluid, set);
        }
        return set;
    }

    /** Retrieves every {@link Item} that has been registered as an empty container with mapContainer or any of the
     * "register*" methods. (In other words a set of every item that is guaranteed to have a {@link GroupedFluidInv} as
     * one of it's attributes).
     * 
     * @return An unmodifiable view of the set of every {@link Item} which can have fluid filled into them, but are
     *         currently empty. (Which also updates as new entries are registered). */
    public static Set<Item> getEmptyContainers() {
        return emptyItemsUn;
    }

    /** Retrieves every {@link Item} that has been registered with {@link #mapContainer(Item, Item, FluidVolume)} as a
     * direct container that is full.
     * 
     * @return An unmodifiable view of the set of every {@link Item} which contain fluid. (Which also updates as new
     *         entries are registered). */
    public static Set<Item> getFullContainers() {
        return fullItemsUn;
    }

    /** Retrieves the {@link FluidVolume} contained by the given {@link Item}, if the item has been registered directly
     * with {@link #mapContainer(Item, Item, FluidVolume)}. (In other words: this doesn't work for potions or other
     * complex items that store fluid data in NBT).
     * 
     * @return A copy of the contained {@link FluidVolume}, or an empty {@link FluidVolume} if the item hasn't been
     *         mapped. */
    public static FluidVolume getContainedFluid(Item item) {
        ItemContainerState state = containerStates.get(item);
        if (state instanceof StateFull) {
            return ((StateFull) state).containedFluid.copy();
        }
        return FluidVolumeUtil.EMPTY;
    }

    // ################
    //
    // Public classes
    //
    // ################

    /** Fill handler for an empty {@link ItemStack}. This is registered with. Alternatively you can implement the
     * functional interface variant {@link FluidFillFunction}. */
    public static abstract class FluidFillHandler {

        public final FluidAmount minimum;

        public FluidFillHandler(FluidAmount minimum) {
            this.minimum = minimum;
        }

        /** Stack return function for
         * {@link FluidFillHandler#insert(ItemStack, FluidVolume, Simulation, StackReturnFunc)} */
        public interface StackReturnFunc {

            /** Sets the {@link ItemStack}s back to where they came. (Internally this is just a delegate to
             * {@link AbstractItemBasedAttribute#setStacks(Simulation, ItemStack, ItemStack)}).
             * <p>
             * This function may only be called once, as it is based on
             * {@link LimitedConsumer#offer(Object, Simulation)} - which doesn't have any defined way to simulate
             * multiple offers.
             * 
             * @return True if the stacks where both accepted, or false if they were both denied. */
            boolean returnStacks(ItemStack oldStack, ItemStack excess);
        }

        /** @param stack A copy of the {@link ItemStack} that was stored in the {@link StackReference}.
         * @param fluid The fluid to insert. (So you should always copy this rather than modify it). Note that this will
         *            never be empty, or have an amount less than {@link #minimum}.
         * @param simulation
         * @param stackReturn A function to set the new stacks to.
         * @return The excess {@link FluidVolume} that couldn't be inserted. */
        protected abstract FluidVolume insert(
            ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn
        );

        /** @param filter the fluids to test.
         * @return */
        protected abstract FluidAmount getCapacity(FluidFilter filter);
    }

    /** {@link FunctionalInterface} version of {@link FluidFillHandler}. */
    @FunctionalInterface
    public interface FluidFillFunction {

        /** @param stack A copy of the {@link ItemStack} that was stored in the {@link StackReference}.
         * @param fluid The fluid to insert. (So you should always copy this rather than modify it). Note that this will
         *            never be empty, or have an amount less than the minimum.
         * @param simulation
         * @param stackReturn A function to set the new stacks to.
         * @return The excess {@link FluidVolume} that couldn't be inserted. */
        FluidVolume insert(ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn);
    }

    // #############
    //
    // Impl classes
    //
    // #############

    static final class NullFluidFillHandler extends FluidFillHandler {
        static final NullFluidFillHandler INSTANCE = new NullFluidFillHandler();

        private NullFluidFillHandler() {
            super(FluidAmount.MAX_BUCKETS);
        }

        @Override
        protected FluidVolume insert(
            ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn
        ) {
            return fluid;
        }

        @Override
        protected FluidAmount getCapacity(FluidFilter filter) {
            return FluidAmount.ZERO;
        }
    }

    /** Wrapper for {@link FluidFillFunction}. */
    static final class FluidFillHandlerWrapper extends FluidFillHandler {

        final FluidAmount capacity;
        final FluidFillFunction function;

        FluidFillHandlerWrapper(FluidAmount minimum, FluidAmount capacity, FluidFillFunction function) {
            super(minimum);
            this.capacity = capacity;
            this.function = function;
        }

        @Override
        protected FluidVolume insert(
            ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn
        ) {
            return function.insert(stack, fluid, simulation, stackReturn);
        }

        @Override
        public String toString() {
            return function.toString();
        }

        @Override
        protected FluidAmount getCapacity(FluidFilter filter) {
            return capacity;
        }
    }

    static final class SimpleDirectFillHandler extends FluidFillHandler {
        final FluidAmount amount;
        final Item filledItem;

        public SimpleDirectFillHandler(FluidAmount amount, Item filledItem) {
            super(amount);
            this.amount = amount;
            this.filledItem = filledItem;
        }

        @Override
        protected FluidVolume insert(
            ItemStack stack, FluidVolume fluid, Simulation simulation, StackReturnFunc stackReturn
        ) {
            ItemStack oldStack = stack.copy();
            ItemStack newStack = new ItemStack(filledItem);
            oldStack.decrement(1);
            FluidVolume newFluid = fluid.copy();
            newFluid.split(amount);
            return stackReturn.returnStacks(oldStack, newStack) ? newFluid : fluid;
        }

        @Override
        protected FluidAmount getCapacity(FluidFilter filter) {
            return amount;
        }
    }

    static abstract /* sealed */ class ItemContainerState /* permits StateEmpty, StateFull */ {
        final Item item;

        ItemContainerState(Item item) {
            this.item = item;
        }

        static String itemToString(Item item) {
            Identifier id = Registry.ITEM.getId(item);
            if (id == null || Registry.ITEM.getDefaultId().equals(id)) {
                return "{UnregisteredItem " + item.toString() + "}";
            }
            return "{Item " + id + "}";
        }

        final String itemToString() {
            return itemToString(item);
        }

        abstract void addItemAttributes();
    }

    static final class StateEmpty extends ItemContainerState {
        final CompatLeveledMap<FluidKey, FluidKey, FluidFillHandler> variants;

        FluidAmount minimum;

        StateEmpty(Item item) {
            super(item);

            variants = new CompatLeveledMap<>(
                "{fluid fill handler for " + itemToString() + "}", FluidKey.class, NullFluidFillHandler.INSTANCE,
                FluidKey::toString
            );
        }

        void mapDirectContainer(Item fullItem, FluidVolume fluid) {
            ItemContainerState fullState = containerStates.get(fullItem);
            if (fullState instanceof StateFull) {
                StateFull state = (StateFull) fullState;
                if (state.emptyItem != item) {
                    LibBlockAttributes.LOGGER.warn(
                        "[FluidContainerRegistry] Rejecting a new mapping with " + itemToString()
                            + " as an empty item to " + state.itemToString()
                            + " because it already has a mapping from a different empty item: "
                            + itemToString(state.emptyItem)
                    );
                    return;
                }

                if (!state.containedFluid.equals(fluid)) {
                    LibBlockAttributes.LOGGER.warn(
                        "[FluidContainerRegistry] Rejecting a new mapping with " + itemToString()
                            + " as an empty item to " + state.itemToString() + " containing " + fluid
                            + " because it already has a mapping to a different fluid: " + state.containedFluid
                    );
                    return;
                }

                // Ignore mapping the same thing twice
                return;
            } else if (fullState != null) {
                // Hard disallow changing whether an item is considered "empty" to "full".
                throw new IllegalArgumentException(
                    "Cannot map " + itemToString() + "as an empty item to " + fullState.itemToString()
                        + " because that item has already been mapped as empty!"
                );
            }

            StateFull state = new StateFull(fullItem, item, fluid);
            containerStates.put(fullItem, state);
            FluidFillHandler handler = new SimpleDirectFillHandler(fluid.amount(), fullItem);
            variants.putExact(AttributeSourceType.INSTANCE, fluid.fluidKey, handler);
            state.addItemAttributes();

            if (minimum == null) {
                minimum = fluid.amount();
            } else {
                minimum = minimum.min(fluid.amount());
            }
        }

        @Override
        void addItemAttributes() {
            FluidAttributes.forEachGroupedInv(attribute -> {
                attribute.setItemAdder(AttributeSourceType.INSTANCE, item, (ref, excess, list) -> {
                    list.offer(new EmptyBucketInv(ref, excess));
                });
            });
        }

        @Override
        public String toString() {
            return "{FluidContainerRegistry.StateEmpty for " + itemToString() + "}";
        }

        class EmptyBucketInv extends AbstractItemBasedAttribute implements GroupedFluidInv {
            public EmptyBucketInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
                super(stackRef, excessStacks);
            }

            @Override
            public Set<FluidKey> getStoredFluids() {
                return Collections.emptySet();
            }

            @Override
            public FluidInvStatistic getStatistics(FluidFilter filter) {
                ItemStack stack = stackRef.get();

                if (stack.isEmpty() || stack.getItem() != item) {
                    return FluidInvStatistic.emptyOf(filter);
                }

                if (filter instanceof ExactFluidFilter) {
                    FluidKey fluid = ((ExactFluidFilter) filter).fluid;
                    FluidFillHandler handler = variants.get(fluid, fluid.getClass());
                    if (handler == null) {
                        return FluidInvStatistic.emptyOf(filter);
                    }
                    FluidAmount capacity = handler.getCapacity(filter).mul(stack.getCount());
                    return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, capacity);
                }

                return new FluidInvStatistic(filter, FluidAmount.ZERO, FluidAmount.ZERO, FluidAmount.NEGATIVE_ONE);
            }

            @Override
            public FluidAmount getAmount_F(FluidKey fluid) {
                return FluidAmount.ZERO;
            }

            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                ItemStack stack = stackRef.get();
                if (stack.isEmpty() || stack.getItem() != item) {
                    return fluid;
                }

                if (fluid.isEmpty()) {
                    return fluid;
                }

                FluidFillHandler handler = variants.get(fluid.fluidKey, fluid.fluidKey.getClass());

                if (handler == null || fluid.amount().isLessThan(handler.minimum)) {
                    return fluid;
                }

                StackReturnFunc stackReturn = new StackReturnFunc() {
                    boolean called = false;

                    @Override
                    public boolean returnStacks(ItemStack oldS, ItemStack newS) {
                        if (called) {
                            throw new IllegalStateException(
                                "Due to the unknowns involved in LimitedConsumer it's not generally useful (or accurate) to call returnStacks multiple times!"
                            );
                        }
                        called = true;
                        return setStacks(simulation, oldS, newS);
                    }
                };
                return handler.insert(stack.copy(), fluid, simulation, stackReturn);
            }

            @Override
            public FluidAmount getMinimumAcceptedAmount() {
                return minimum;
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                return FluidVolumeUtil.EMPTY;
            }

            @Override
            public String toString() {
                return "{FluidContainerRegistry.EmptyBucket for " + itemToString() + " in " + stackRef + "}";
            }
        }
    }

    static final class StateFull extends ItemContainerState {
        final Item emptyItem;
        final FluidVolume containedFluid;

        StateFull(Item fullItem, Item emptyItem, FluidVolume containedFluid) {
            super(fullItem);
            this.emptyItem = emptyItem;
            this.containedFluid = containedFluid;
        }

        void linkFull(Item fullItem, FluidVolume fluid) {
            throw new IllegalArgumentException(
                "The item " + itemToString()
                    + " is already mapped as an empty item - so we cannot remap it as empty to allow it to be filled with "
                    + fluid + " to make " + itemToString(fullItem)
            );
        }

        @Override
        void addItemAttributes() {
            FluidAttributes.forEachGroupedInv(attribute -> {
                attribute.setItemAdder(AttributeSourceType.INSTANCE, item, (ref, excess, list) -> {
                    list.offer(new FullBucketInv(ref, excess));
                });
            });
        }

        @Override
        public String toString() {
            return "{FluidContainerRegistry.StateFull for " + itemToString() + "}";
        }

        class FullBucketInv extends AbstractItemBasedAttribute implements GroupedFluidInv {
            public FullBucketInv(Reference<ItemStack> stackRef, LimitedConsumer<ItemStack> excessStacks) {
                super(stackRef, excessStacks);
            }

            @Override
            public Set<FluidKey> getStoredFluids() {
                ItemStack stack = stackRef.get();
                if (stack.isEmpty() || stack.getItem() != item) {
                    return Collections.emptySet();
                }
                return Collections.singleton(containedFluid.fluidKey);
            }

            @Override
            public FluidInvStatistic getStatistics(FluidFilter filter) {
                ItemStack stack = stackRef.get();
                if (stack.isEmpty() || stack.getItem() != item) {
                    return FluidInvStatistic.emptyOf(filter);
                }
                if (!filter.matches(containedFluid.fluidKey)) {
                    return FluidInvStatistic.emptyOf(filter);
                }

                FluidAmount amount = containedFluid.amount().mul(stack.getCount());
                return new FluidInvStatistic(filter, amount, FluidAmount.ZERO, FluidAmount.ZERO);
            }

            @Override
            public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
                return fluid;
            }

            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                ItemStack stack = stackRef.get();
                if (stack.isEmpty() || stack.getItem() != item) {
                    return FluidVolumeUtil.EMPTY;
                }

                if (maxAmount.isLessThan(containedFluid.amount())) {
                    return FluidVolumeUtil.EMPTY;
                }

                if (!filter.matches(containedFluid.fluidKey)) {
                    return FluidVolumeUtil.EMPTY;
                }

                ItemStack oldStack = stack.copy();
                ItemStack newStack = new ItemStack(emptyItem);
                oldStack.decrement(1);
                if (setStacks(simulation, oldStack, newStack)) {
                    return containedFluid.copy();
                } else {
                    return FluidVolumeUtil.EMPTY;
                }
            }

            @Override
            public String toString() {
                return "{FluidContainerRegistry.FullBucket for " + itemToString() + " in " + stackRef + "}";
            }
        }
    }
}
