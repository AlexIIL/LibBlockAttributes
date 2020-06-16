/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey.FluidKeyBuilder;

public class FluidPropertyTester {

    @Test
    public void testFluidProperty() {
        FluidKey mana = new SimpleFluidKey(
            new FluidKeyBuilder(new Identifier("lba_test:mana"))//
                .setName(new LiteralText("Mana"))//
                .setUnit(FluidUnit.BOTTLE)//
        );
        mana.register();
        mana.forceRegisterProperty(PurityProp.INSTANCE);

        FluidVolume manaA = mana.withAmount(FluidAmount.BOTTLE);

        System.out.println("\nA:");
        for (Text text : manaA.getFullTooltip()) {
            System.out.println(text.getString());
        }

        FluidVolume manaB = mana.withAmount(FluidAmount.BOTTLE);
        manaB.setProperty(PurityProp.INSTANCE, new Purity(0.2, 1, 0));

        System.out.println("\nB:");
        for (Text text : manaB.getFullTooltip()) {
            System.out.println(text.getString());
        }

        FluidVolume manaC = FluidVolume.merge(manaA, manaB);
        if (manaC == null) {
            Assert.fail("manaC == null!");
        } else {
            System.out.println("\nC:");
            for (Text text : manaC.getFullTooltip()) {
                System.out.println(text.getString());
            }
        }
    }

    public static class PurityProp extends FluidProperty<Purity> {

        public static final PurityProp INSTANCE = new PurityProp();

        private PurityProp() {
            super(new Identifier("lba_test:purity"), Purity.class, Purity.NONE);
        }

        @Override
        protected Tag toTag(Purity value) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("Quality", value.quality);
            tag.putDouble("Uniformity", value.uniformity);
            tag.putDouble("Alignment", value.alignment);
            return tag;
        }

        @Override
        protected Purity fromTag(Tag tag) {
            if (tag instanceof CompoundTag) {
                CompoundTag c = (CompoundTag) tag;
                return new Purity(c.getDouble("Quality"), c.getDouble("Uniformity"), c.getDouble("Alignment"));
            } else {
                return Purity.NONE;
            }
        }

        @Override
        protected Purity merge(
            FluidVolume volumeA, FluidVolume volumeB, FluidAmount amount, Purity valueA, Purity valueB
        ) {
            double avgQuality = (valueA.quality * volumeA.getAmount_F().asInexactDouble()//
                + valueB.quality * volumeB.getAmount_F().asInexactDouble()) / amount.asInexactDouble();
            double avgAlignment = (valueA.alignment * volumeA.getAmount_F().asInexactDouble()//
                + valueB.alignment * volumeB.getAmount_F().asInexactDouble()) / amount.asInexactDouble();

            // Calculate how much the quality&alignment have drifted
            double diffQuality = Math.abs(valueA.quality - valueB.quality);
            double diffAlign = Math.abs(valueA.alignment - valueB.alignment);
            double diffTotal = diffQuality + diffAlign;

            double uniformity = (valueA.uniformity + valueB.uniformity + 2 - diffTotal) / 4;

            return new Purity(avgQuality, uniformity, avgAlignment);
        }

        @Override
        public void addTooltipExtras(FluidVolume fluid, FluidTooltipContext context, List<Text> tooltip) {
            Purity purity = get(fluid);
            tooltip.add(new LiteralText("Quality = " + (int) (purity.quality * 1000) / 10.0 + "%"));
            tooltip.add(new LiteralText("Uniformity = " + (int) (purity.uniformity * 1000) / 10.0 + "%"));
            tooltip.add(new LiteralText("Alignment = " + (int) (purity.alignment * 1000) / 10.0 + "%"));
        }
    }

    public static final class Purity {
        public static final Purity NONE = new Purity(0, 1, 0);

        /** 0 to 1 */
        public final double quality, uniformity, alignment;

        public Purity(double quality, double uniformity, double alignment) {
            this.quality = quality;
            this.uniformity = uniformity;
            this.alignment = alignment;
        }

        @Override
        public int hashCode() {
            return Objects.hash(quality, uniformity, alignment);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Purity)) {
                return false;
            }
            Purity other = (Purity) obj;
            return quality == other.quality && uniformity == other.uniformity && alignment == other.alignment;
        }
    }
}
