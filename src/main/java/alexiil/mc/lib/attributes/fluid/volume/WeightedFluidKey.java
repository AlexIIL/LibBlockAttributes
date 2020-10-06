/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
    @Deprecated // (since = "0.6.0", forRemoval = true)
    public WeightedFluidVolume<T> withAmount(int amount) {
        return withAmount(FluidAmount.of1620(amount));
    }

    public abstract WeightedFluidVolume<T> withAmount(T value, FluidAmount amount);

    @Deprecated // (since = "0.6.4", forRemoval = true)
    public WeightedFluidVolume<T> withAmount(T value, int amount) {
        return withAmount(value, FluidAmount.of1620(amount));
    }
}
