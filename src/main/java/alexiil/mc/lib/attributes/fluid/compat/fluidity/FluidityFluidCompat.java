package alexiil.mc.lib.attributes.fluid.compat.fluidity;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProvider;
import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilterUtil;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

import grondag.fluidity.api.article.Article;
import grondag.fluidity.api.device.DeviceComponentAccess;
import grondag.fluidity.api.fraction.Fraction;
import grondag.fluidity.api.storage.Store;

/* package-private */ final class FluidityFluidCompat {
    private FluidityFluidCompat() {}

    static void load() {
        FluidAttributes.INSERTABLE.appendBlockAdder(FluidityFluidCompat::add);
        FluidAttributes.EXTRACTABLE.appendBlockAdder(FluidityFluidCompat::add);
        // FluidAttributes.GROUPED_INV_VIEW.appendBlockAdder(FluidityFluidCompat::add);
        // FluidAttributes.GROUPED_INV.appendBlockAdder(FluidityFluidCompat::add);
        // FluidAttributes.FIXED_INV_VIEW.appendBlockAdder(FluidityFluidCompat::add);
        // FluidAttributes.FIXED_INV.appendBlockAdder(FluidityFluidCompat::add);
    }

    private static Fraction convert(FluidAmount fa) {
        return Fraction.of(fa.whole, fa.numerator, fa.denominator);
    }

    private static FluidAmount convert(Fraction fa) {
        return FluidAmount.of(fa.whole(), fa.numerator(), fa.divisor());
    }

    @Nullable
    private static Article article(FluidKey key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Fluid raw = key.getRawFluid();
        if (raw == null) {
            return null;
        }
        return Article.of(raw);
    }

    private static void add(
        World world, BlockPos pos, BlockState state, AttributeList<? super FluidityStoreWrapper> list
    ) {
        if (state.getBlock() instanceof AttributeProvider) {
            return;
        }

        DeviceComponentAccess<Store> access = Store.STORAGE_COMPONENT.getAccess(world, pos, state);

        final Store store;
        Direction searchDir = list.getSearchDirection();
        if (searchDir != null) {
            store = access.get(searchDir.getOpposite());
        } else {
            store = access.get();
        }
        if (store == Store.EMPTY) {
            return;
        }
        // TODO: Some way of checking to see if this actually works with fluids!
        list.add(new FluidityStoreWrapper(store));
    }

    static final class FluidityStoreWrapper implements GroupedFluidInv, FixedFluidInv {
        final Store store;

        public FluidityStoreWrapper(Store store) {
            this.store = store;
        }

        // FluidInsertable

        @Override
        public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
            if (fluid.isEmpty()) {
                return fluid;
            }
            Article article = article(fluid.fluidKey);
            if (article == null) {
                return fluid;
            }
            Fraction volume = convert(fluid.getAmount_F());
            Fraction added = store.getConsumer().apply(article, volume, simulation.isSimulate());
            if (added.isZero()) {
                return fluid;
            }
            fluid = fluid.copy();
            return fluid.split(convert(added));
        }

        // FluidExtractable

        @Override
        public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
            if (filter == ConstantFluidFilter.ANYTHING) {
                return attemptAnyExtraction(maxAmount, simulation);
            }
            if (filter == ConstantFluidFilter.NOTHING) {
                return FluidVolumeUtil.EMPTY;
            }

            // Fluidity doesn't seem to expose filtered extractions
            // so instead we have to decompose it and try everything
            Set<FluidKey> keys = FluidFilterUtil.decomposeFilter(filter);
            if (keys == null) {
                return FluidVolumeUtil.EMPTY;
            }
            for (FluidKey key : keys) {
                Article article = article(key);
                if (article == null) {
                    continue;
                }

            }
            return FluidVolumeUtil.EMPTY;
        }

        @Override
        public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        // GroupedFluidInv

        @Override
        public Set<FluidKey> getStoredFluids() {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        @Override
        public FluidInvStatistic getStatistics(FluidFilter filter) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        @Override
        public FluidAmount getAmount_F(FluidKey fluid) {
            Article article = article(fluid);
            if (article == null) {
                return FluidAmount.ZERO;
            }
            return convert(store.amountOf(article));
        }

        @Override
        public FluidAmount getCapacity_F(FluidKey fluid) {
            Article article = article(fluid);
            if (article == null) {
                return FluidAmount.ZERO;
            }
            return FluidAmount.ofWhole(store.capacity());
        }

        // FixedFluidInv

        @Override
        public int getTankCount() {
            return Math.max(0, store.handleCount());
        }

        @Override
        public FluidVolume getInvFluid(int tank) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        @Override
        public boolean isFluidValidForTank(int tank, FluidKey fluid) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        @Override
        public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }

        @Override
        public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
            // TODO Auto-generated method stub
            throw new AbstractMethodError("// TODO: Implement this!");
        }
    }
}
