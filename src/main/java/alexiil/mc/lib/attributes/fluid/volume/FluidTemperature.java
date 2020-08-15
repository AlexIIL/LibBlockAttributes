/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;

import net.minecraft.text.Text;

/** Base type for fluid temperature. There are 2 types of temperature, only one of which may be implemented:
 * <ul>
 * <li>{@link DiscreteFluidTemperature}, for {@link FluidKey} based temperatures.</li>
 * <li>{@link ContinuousFluidTemperature}, for {@link FluidVolume} or {@link FluidProperty} based temperatures.</li>
 * </ul>
 * It is considered an error to implement this interface without implementing one of these two subinterfaces!
 * <p>
 * Note that LibNetworkStack doesn't (yet) provide a way to heat or cool fluids directly. As such this is provided for
 * informational purposes only, and to ensure that each {@link FluidKey} only has a single temperature. */
public interface FluidTemperature {
    // TODO: Fluid temperature transference using FluidKey.thermalCapacity

    /** @return The temperature of the fluid, in degrees C.
     * @throws IllegalArgumentException if this temperature scale doesn't apply to the given {@link FluidVolume}. */
    double getTemperature(FluidVolume fluid);

    default void addTemperatureToTooltip(FluidKey fluid, FluidTooltipContext context, List<Text> tooltip) {}

    default void addTemperatureToTooltip(FluidVolume fluid, FluidTooltipContext context, List<Text> tooltip) {
        addTemperatureToTooltip(fluid.fluidKey, context, tooltip);
    }

    /** Validates that the given temperature is either null or implements exactly 1 of {@link DiscreteFluidTemperature}
     * or {@link ContinuousFluidTemperature}. */
    public static void validate(FluidTemperature temperature) {
        if (temperature == null) {
            return;
        }

        boolean discrete = temperature instanceof DiscreteFluidTemperature;
        boolean cont = temperature instanceof ContinuousFluidTemperature;

        if (discrete & cont) {
            throw new IllegalStateException(
                "The temperature " + temperature.getClass()
                    + " implements both DiscreteFluidTemperature and ContinuousFluidTemperature, which it should only implement one!"
            );
        }

        if (discrete == cont) {
            throw new IllegalStateException(
                "The temperature " + temperature.getClass()
                    + " must implement one of DiscreteFluidTemperature or ContinuousFluidTemperature, but it implements neither!"
            );
        }
    }

    /** A Discrete {@link FluidTemperature} has a single temperature per {@link FluidKey}. */
    public interface DiscreteFluidTemperature extends FluidTemperature {

        /** @throws IllegalArgumentException if this temperature scale doesn't apply to the given {@link FluidKey}. */
        double getTemperature(FluidKey fluidKey);

        @Override
        default double getTemperature(FluidVolume fluid) {
            return getTemperature(fluid.fluidKey);
        }
    }

    /** A Continuous {@link FluidTemperature} can have a range of temperatures for a single fluid. */
    public interface ContinuousFluidTemperature extends FluidTemperature {
        @Override
        double getTemperature(FluidVolume fluid);
    }
}
