package alexiil.mc.lib.attributes.fluid.volume;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public abstract class WeightedFluidKey<T> extends FluidKey {

    public final Class<T> valueClass;
    public final T defaultValue;

    public WeightedFluidKey(FluidKeyBuilder builder, Class<T> valueClass, T defaultValue) {
        super(builder);
        this.valueClass = valueClass;
        this.defaultValue = defaultValue;
    }

    @Override
    public WeightedFluidVolume<T> withAmount(FluidAmount amount) {
        return withAmount(defaultValue, amount);
    }

    @Override
    @Deprecated
    public WeightedFluidVolume<T> withAmount(int amount) {
        return withAmount(FluidAmount.of1620(amount));
    }

    public abstract WeightedFluidVolume<T> withAmount(T value, FluidAmount amount);

    @Deprecated
    public WeightedFluidVolume<T> withAmount(T value, int amount) {
        return withAmount(value, FluidAmount.of1620(amount));
    }
}
