package alexiil.mc.lib.attributes.fluid.compat.silk;

import io.github.prospector.silk.fluid.FluidContainer;
import io.github.prospector.silk.fluid.FluidContainerProvider;
import io.github.prospector.silk.fluid.FluidInstance;
import io.github.prospector.silk.util.ActionType;

import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.IFluidExtractable;
import alexiil.mc.lib.attributes.fluid.filter.IFluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.NormalFluidVolume;

public class SilkFluidCompat {
    public static void load() {
        FluidAttributes.INSERTABLE.appendCustomAdder((world, pos, state, list) -> {
            if (state.getBlock() instanceof FluidContainerProvider) {
                FluidContainerProvider provider = (FluidContainerProvider) state.getBlock();
                FluidContainer container = provider.getContainer(state, world, pos);
                if (container != null) {
                    list.add((fluid, simulation) -> {
                        Direction dir = list.getSearchDirection();
                        if (dir != null) {
                            dir = dir.getOpposite();
                        }
                        if (fluid instanceof NormalFluidVolume) {
                            // Silk requires normal minecraft fluids
                            NormalFluidVolume normalFluid = (NormalFluidVolume) fluid;

                            int amountMoved = container.tryPartialInsertFluid(dir, normalFluid.getRawFluid(),
                                normalFluid.getAmount(), toSilkAction(simulation));
                            fluid = fluid.copy();
                            FluidVolume removed = fluid.split(amountMoved);
                            assert removed.getAmount() == amountMoved;
                        }
                        return fluid;
                    });
                }
            }
        });

        FluidAttributes.EXTRACTABLE.appendCustomAdder((world, pos, state, list) -> {
            if (state.getBlock() instanceof FluidContainerProvider) {
                FluidContainerProvider provider = (FluidContainerProvider) state.getBlock();
                FluidContainer container = provider.getContainer(state, world, pos);
                if (container != null) {
                    list.add(new IFluidExtractable() {
                        @Override
                        public FluidVolume attemptExtraction(IFluidFilter filter, int maxAmount,
                            Simulation simulation) {
                            Direction dir = list.getSearchDirection();
                            if (dir != null) {
                                dir = dir.getOpposite();
                            }
                            for (FluidInstance containedFluid : container.getFluids(dir)) {
                                Fluid rawFluid = containedFluid.getFluid();
                                FluidKey fluidKey = FluidKeys.get(rawFluid);
                                if (fluidKey != null && filter.matches(fluidKey)) {
                                    int extracted = container.tryPartialExtractFluid(dir, rawFluid, maxAmount,
                                        toSilkAction(simulation));
                                    if (extracted > 0) {
                                        return fluidKey.withAmount(extracted);
                                    }
                                }
                            }
                            return FluidKeys.EMPTY.withAmount(0);
                        }
                    });
                }
            }
        });
    }

    private static ActionType toSilkAction(Simulation simulation) {
        return simulation == Simulation.ACTION ? ActionType.PERFORM : ActionType.SIMULATE;
    }
}
