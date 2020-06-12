/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import javax.annotation.Nullable;

import net.minecraft.text.Text;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** Base class for {@link FluidUnit} and {@link FluidUnitSet}. */
public abstract class FluidUnitBase {
    /* package-private */ FluidUnitBase() {}

    private static final FluidTooltipContext CTX = FluidTooltipContext.USE_CONFIG;

    // Variants:
    // (FluidAmount args) -> string
    // (FluidAmount args) -> Text
    // (FluidAmount args, name) -> string
    // (FluidAmount args, name) -> Text
    // (FluidAmount args, ctx) -> string
    // (FluidAmount args, ctx) -> Text
    // (FluidAmount args, name, ctx) -> string
    // (FluidAmount args, name, ctx) -> Text

    // TODO: Add a fluid name variant
    // also remove the deprecated (int) variants
    // as they are much less necessary than the others
    // also, is that it?
    // because if there are others then I kinda want to know about them...

    // #####################
    // *amount(amount)
    // #####################

    public final String localizeAmount(FluidAmount amount) {
        return localizeAmount(amount, false, null, CTX);
    }

    public final Text getAmount(FluidAmount amount) {
        return getAmount(amount, false, null, CTX);
    }

    public final String localizeAmount(FluidAmount amount, @Nullable Text fluidName) {
        return localizeAmount(amount, false, fluidName, CTX);
    }

    public final Text getAmount(FluidAmount amount, @Nullable Text fluidName) {
        return getAmount(amount, false, fluidName, CTX);
    }

    public final String localizeAmount(FluidAmount amount, FluidTooltipContext ctx) {
        return localizeAmount(amount, false, null, ctx);
    }

    public final Text getAmount(FluidAmount amount, FluidTooltipContext ctx) {
        return getAmount(amount, false, null, ctx);
    }

    public final String localizeAmount(FluidAmount amount, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return localizeAmount(amount, false, fluidName, ctx);
    }

    public final Text getAmount(FluidAmount amount, @Nullable Text fluidName, FluidTooltipContext ctx) {
        return getAmount(amount, false, fluidName, ctx);
    }

    // ################################
    // *amount(amount, forceSingular)
    // ################################

    public final String localizeAmount(FluidAmount amount, boolean forceSingular) {
        return localizeAmount(amount, forceSingular, null, CTX);
    }

    public final Text getAmount(FluidAmount amount, boolean forceSingular) {
        return getAmount(amount, forceSingular, null, CTX);
    }

    public final String localizeAmount(FluidAmount amount, boolean forceSingular, @Nullable Text fluidName) {
        return localizeAmount(amount, forceSingular, fluidName, CTX);
    }

    public final Text getAmount(FluidAmount amount, boolean forceSingular, @Nullable Text fluidName) {
        return getAmount(amount, forceSingular, fluidName, CTX);
    }

    public final String localizeAmount(FluidAmount amount, boolean forceSingular, FluidTooltipContext ctx) {
        return localizeAmount(amount, forceSingular, null, ctx);
    }

    public final Text getAmount(FluidAmount amount, boolean forceSingular, FluidTooltipContext ctx) {
        return getAmount(amount, forceSingular, null, ctx);
    }

    public abstract String localizeAmount(FluidAmount amount, boolean forceSingular, @Nullable Text fluidName, FluidTooltipContext ctx);

    public abstract Text getAmount(FluidAmount amount, boolean forceSingular, @Nullable Text fluidName, FluidTooltipContext ctx);

    // ################################
    // *emptyTank(capacity)
    // ################################

    public final String localizeEmptyTank(FluidAmount capacity) {
        return localizeEmptyTank(capacity, CTX);
    }

    public final Text getEmptyTank(FluidAmount capacity) {
        return getEmptyTank(capacity, CTX);
    }

    public abstract String localizeEmptyTank(FluidAmount capacity, FluidTooltipContext ctx);

    public abstract Text getEmptyTank(FluidAmount capacity, FluidTooltipContext ctx);

    // ################################
    // *fullTank(capacity)
    // ################################

    public final String localizeFullTank(FluidAmount capacity) {
        return localizeFullTank(capacity, null, CTX);
    }

    public final Text getFullTank(FluidAmount capacity) {
        return getFullTank(capacity, null, CTX);
    }

    public final String localizeFullTank(FluidAmount capacity, @Nullable Text fluidName) {
        return localizeFullTank(capacity, fluidName, CTX);
    }

    public final Text getFullTank(FluidAmount capacity, @Nullable Text fluidName) {
        return getFullTank(capacity, fluidName, CTX);
    }

    public final String localizeFullTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return localizeFullTank(capacity, null, ctx);
    }

    public final Text getFullTank(FluidAmount capacity, FluidTooltipContext ctx) {
        return getFullTank(capacity, null, ctx);
    }

    public abstract String localizeFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx);

    public abstract Text getFullTank(FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx);

    // #######################################
    // *partialTank(amount, capacity)
    // #######################################
    
    public final String localizePartialTank(FluidAmount amount, FluidAmount capacity) {
        return localizePartialTank(amount, capacity, null, CTX);
    }

    public final Text getPartialTank(FluidAmount amount, FluidAmount capacity) {
        return getPartialTank(amount, capacity, null, CTX);
    }

    public final String localizePartialTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName) {
        return localizePartialTank(amount, capacity, fluidName, CTX);
    }

    public final Text getPartialTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName) {
        return getPartialTank(amount, capacity, fluidName, CTX);
    }

    public final String localizePartialTank(FluidAmount amount, FluidAmount capacity, FluidTooltipContext ctx) {
        return localizePartialTank(amount, capacity, null, ctx);
    }

    public final Text getPartialTank(FluidAmount amount, FluidAmount capacity, FluidTooltipContext ctx) {
        return getPartialTank(amount, capacity, null, ctx);
    }

    public abstract String localizePartialTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx);

    public abstract Text getPartialTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx);

    // ################################
    // *tank(amount, capacity)
    // ################################

    public final String localizeTank(FluidAmount amount, FluidAmount capacity) {
        return localizeTank(amount, capacity, null, CTX);
    }

    public final Text getTank(FluidAmount amount, FluidAmount capacity) {
        return getTank(amount, capacity, null, CTX);
    }

    public final String localizeTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName) {
        return localizeTank(amount, capacity, fluidName, CTX);
    }

    public final Text getTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName) {
        return getTank(amount, capacity, fluidName, CTX);
    }

    public final String localizeTank(FluidAmount amount, FluidAmount capacity, FluidTooltipContext ctx) {
        return localizeTank(amount, capacity, null, ctx);
    }

    public final Text getTank(FluidAmount amount, FluidAmount capacity, FluidTooltipContext ctx) {
        return getTank(amount, capacity, null, ctx);
    }

    public final String localizeTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        if (amount.isZero()) {
            return localizeEmptyTank(capacity, ctx);
        } else if (amount.equals(capacity)) {
            return localizeFullTank(capacity, fluidName, ctx);
        } else {
            return localizePartialTank(amount, capacity, fluidName, ctx);
        }
    }

    public final Text getTank(FluidAmount amount, FluidAmount capacity, @Nullable Text fluidName, FluidTooltipContext ctx) {
        if (amount.isZero()) {
            return getEmptyTank(capacity, ctx);
        } else if (amount.equals(capacity)) {
            return getFullTank(capacity, fluidName, ctx);
        } else {
            return getPartialTank(amount, capacity, fluidName, ctx);
        }
    }

    // ################################
    // *flowRate(amount)
    // ################################

    public final String localizeFlowRate(FluidAmount amountPerTick) {
        return localizeFlowRate(amountPerTick, null, CTX);
    }

    public final Text getFlowRate(FluidAmount amountPerTick) {
        return getFlowRate(amountPerTick, null, CTX);
    }

    public final String localizeFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName) {
        return localizeFlowRate(amountPerTick, fluidName, CTX);
    }

    public final Text getFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName) {
        return getFlowRate(amountPerTick, fluidName, CTX);
    }

    public final String localizeFlowRate(FluidAmount amountPerTick, FluidTooltipContext ctx) {
        return localizeFlowRate(amountPerTick, null, ctx);
    }

    public final Text getFlowRate(FluidAmount amountPerTick, FluidTooltipContext ctx) {
        return getFlowRate(amountPerTick, null, ctx);
    }

    public abstract String localizeFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx);

    public abstract Text getFlowRate(FluidAmount amountPerTick, @Nullable Text fluidName, FluidTooltipContext ctx);
}
