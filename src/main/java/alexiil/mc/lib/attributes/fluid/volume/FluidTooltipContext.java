/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import alexiil.mc.lib.attributes.fluid.LbaFluidsConfig;
import alexiil.mc.lib.attributes.fluid.init.LbaFluidProxy;

/** Stores various options for creating tooltips for fluids. This is meant to wrap a {@link TooltipContext}, but has a
 * lot more options specific to fluids and isn't limited to the client side only. */
public final class FluidTooltipContext {

    private static final int VALUE_FLAGS = 0b11;
    private static final int VALUE_USE_CONFIG = 0b00;
    private static final int VALUE_ENABLED = 0b10;
    private static final int VALUE_DISABLED = 0b11;

    private static final int OFFSET_ADVANCED = 0;
    private static final int OFFSET_SYMBOLS = 2;
    private static final int OFFSET_TICKS = 4;
    private static final int OFFSET_SHORT_DESC = 6;
    private static final int OFFSET_DISABLE_FLUID_COLOURS = 8;
    private static final int OFFSET_DISABLE_EMPHASIS_COLOURS = 10;
    private static final int OFFSET_JOIN_NAME_AMOUNT = 12;

    /** The default context, that lets the user decide how they want to see tooltips. */
    public static final FluidTooltipContext USE_CONFIG = new FluidTooltipContext(0);

    private final long options;

    private FluidTooltipContext(long values) {
        this.options = values;
    }

    public static FluidTooltipContext fromMinecraft(TooltipContext ctx) {
        return USE_CONFIG.forceAdvanced(ctx.isAdvanced());
    }

    // #######
    // Getters
    // #######

    /** @return True to add advanced details, or false otherwise. Note that this returns
     *         {@link GameOptions#advancedItemTooltips} by default. */
    public boolean isAdvanced() {
        long o = (options >> OFFSET_ADVANCED) & VALUE_FLAGS;
        if (o != VALUE_USE_CONFIG) {
            return o == VALUE_ENABLED;
        } else {
            return LbaFluidProxy.MC_TOOLTIPS_ADVANCED.getAsBoolean();
        }
    }

    public boolean shouldUseSymbols() {
        return get(OFFSET_SYMBOLS, LbaFluidsConfig.USE_SYMBOLS);
    }

    public boolean shouldUseTicks() {
        return get(OFFSET_TICKS, LbaFluidsConfig.USE_TICKS);
    }

    /** @return The number of ticks between each unit of time. (so 1 for ticks, and 20 for seconds). */
    public int getTimeGap() {
        return shouldUseTicks() ? 1 : 20;
    }

    public boolean shouldUseShortDescription() {
        return get(OFFSET_SHORT_DESC, LbaFluidsConfig.USE_SHORT_DESC);
    }

    public boolean shouldStripFluidColours() {
        return get(OFFSET_DISABLE_FLUID_COLOURS, LbaFluidsConfig.DISABLE_FLUID_COLOURS);
    }

    public boolean shouldDisableEmphasisColours() {
        return get(OFFSET_DISABLE_EMPHASIS_COLOURS, LbaFluidsConfig.DISABLE_EMPHASIS_COLOURS);
    }

    public boolean shouldJoinNameWithAmount() {
        return get(OFFSET_JOIN_NAME_AMOUNT, LbaFluidsConfig.TOOLTIP_JOIN_NAME_AMOUNT);
    }

    private boolean get(int offset, boolean config) {
        long o = (options >> offset) & VALUE_FLAGS;
        if (o != VALUE_USE_CONFIG) {
            return o == VALUE_ENABLED;
        } else {
            return config;
        }
    }

    // ########
    // Deriving
    // ########

    /** @return A {@link FluidTooltipContext} which uses
     *         {@link MinecraftClient#options}.{@link GameOptions#advancedItemTooltips advancedItemTooltips} for
     *         {@link #isAdvanced()}. */
    public FluidTooltipContext usingMcAdvanced() {
        return withConfig(OFFSET_ADVANCED);
    }

    /** @return A {@link FluidTooltipContext} which uses the given value for {@link #isAdvanced()}. */
    public FluidTooltipContext forceAdvanced(boolean enable) {
        return withForced(OFFSET_ADVANCED, enable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for {@link #shouldUseSymbols()}. */
    public FluidTooltipContext usingConfigForSymbols() {
        return withConfig(OFFSET_SYMBOLS);
    }

    public FluidTooltipContext forceSymbols(boolean enable) {
        return withForced(OFFSET_SYMBOLS, enable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for {@link #shouldUseTicks()}. */
    public FluidTooltipContext usingConfigForTicks() {
        return withConfig(OFFSET_TICKS);
    }

    public FluidTooltipContext forceTicks(boolean enable) {
        return withForced(OFFSET_TICKS, enable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for {@link #shouldUseTicks()}. */
    public FluidTooltipContext usingConfigForShortDesc() {
        return withConfig(OFFSET_SHORT_DESC);
    }

    public FluidTooltipContext forceShortDesc(boolean enable) {
        return withForced(OFFSET_SHORT_DESC, enable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for
     *         {@link #shouldStripFluidColours()}. */
    public FluidTooltipContext usingConfigForFluidColours() {
        return withConfig(OFFSET_DISABLE_FLUID_COLOURS);
    }

    public FluidTooltipContext forceDisableFluidColours(boolean disable) {
        return withForced(OFFSET_DISABLE_FLUID_COLOURS, disable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for
     *         {@link #shouldDisableEmphasisColours()}. */
    public FluidTooltipContext usingConfigForEmphasisColours() {
        return withConfig(OFFSET_DISABLE_EMPHASIS_COLOURS);
    }

    public FluidTooltipContext forceDisableEmphasisColours(boolean disable) {
        return withForced(OFFSET_DISABLE_EMPHASIS_COLOURS, disable);
    }

    /** @return A {@link FluidTooltipContext} which uses the LBA fluids config file for
     *         {@link #shouldJoinNameWithAmount()}. */
    public FluidTooltipContext usingConfigForJoinedName() {
        return withConfig(OFFSET_JOIN_NAME_AMOUNT);
    }

    public FluidTooltipContext forceJoinedName(boolean enable) {
        return withForced(OFFSET_JOIN_NAME_AMOUNT, enable);
    }

    private FluidTooltipContext withConfig(int offset) {
        long newOptions = options & ~(VALUE_FLAGS << offset);
        return options == newOptions ? this : new FluidTooltipContext(newOptions);
    }

    private FluidTooltipContext withForced(int offset, boolean enable) {
        long cleared = options & ~(VALUE_FLAGS << offset);
        long newOptions = cleared | ((enable ? VALUE_ENABLED : VALUE_DISABLED) << offset);
        return options == newOptions ? this : new FluidTooltipContext(newOptions);
    }

    // ########
    // Utils
    // ########

    /** Strips any colours from the given name if {@link #shouldStripFluidColours()} is true, otherwise returns the
     * fluid name as-is. */
    public Text stripFluidColours(Text fluidName) {
        if (shouldStripFluidColours()) {
            return new LiteralText(fluidName.getString());
        } else {
            return fluidName;
        }
    }
}
