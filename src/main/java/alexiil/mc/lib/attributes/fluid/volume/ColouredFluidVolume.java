/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.math.RoundingMode;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.JsonHelper;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount.FluidMergeResult;

/** A fluid volume that stores it's colours as 4 floats: red, green, blue, and alpha. (Alpha bounds for blending can be
 * configured in the fluid key). */
public class ColouredFluidVolume extends FluidVolume {

    float red = getFluidKey().defaultRed;
    float green = getFluidKey().defaultGreen;
    float blue = getFluidKey().defaultBlue;
    float alpha = getFluidKey().defaultAlpha;

    public ColouredFluidVolume(ColouredFluidKey key, FluidAmount amount) {
        super(key, amount);
    }

    @Deprecated  // in 0.6.4
    public ColouredFluidVolume(ColouredFluidKey key, int amount) {
        super(key, amount);
    }

    public ColouredFluidVolume(ColouredFluidKey key, NbtCompound tag) {
        super(key, tag);

        if (tag.contains("colour")) {
            NbtList list = tag.getList("colour", NbtFloat.ZERO.getType());
            if (list.size() == 4) {
                red = Math.max(key.minRed, Math.min(key.maxRed, list.getFloat(0)));
                green = Math.max(key.minGreen, Math.min(key.maxGreen, list.getFloat(1)));
                blue = Math.max(key.minBlue, Math.min(key.maxBlue, list.getFloat(2)));
                alpha = Math.max(key.minAlpha, Math.min(key.maxAlpha, list.getFloat(3)));
            }
        }
    }

    @Override
    public NbtCompound toTag(NbtCompound nbt) {
        super.toTag(nbt);
        NbtList list = new NbtList();
        list.add(NbtFloat.of(red));
        list.add(NbtFloat.of(green));
        list.add(NbtFloat.of(blue));
        list.add(NbtFloat.of(alpha));
        nbt.put("colour", list);
        return nbt;
    }

    public ColouredFluidVolume(ColouredFluidKey key, JsonObject json) throws JsonSyntaxException {
        super(key, json);
        JsonArray colour = JsonHelper.getArray(json, "color", JsonHelper.getArray(json, "colour", new JsonArray()));
        if (colour != null && (colour.size() == 3 || colour.size() == 4)) {
            float r = getColourPart(colour, 0);
            float g = getColourPart(colour, 1);
            float b = getColourPart(colour, 2);
            float a = 1;
            if (colour.size() == 4) {
                a = getColourPart(colour, 3);
            }
            setRgba(r / 255, g / 255, b / 255, a / 255);
        }
    }

    private static float getColourPart(JsonArray array, int index) {
        JsonElement elem = array.get(index);
        if (!elem.isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected a primitive, but got " + elem + " at [" + index + "]");
        }
        JsonPrimitive p = elem.getAsJsonPrimitive();
        if (p.isNumber()) {
            return p.getAsFloat();
        }

        try {
            return p.getAsFloat();
        } catch (NumberFormatException nfe) {
            throw new JsonSyntaxException("Expected a valid float, but got " + elem + " at [" + index + "]");
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray colour = new JsonArray();
        colour.add(red * 255);
        colour.add(green * 255);
        colour.add(blue * 255);
        colour.add(alpha * 255);
        json.add("colour", colour);
        return json;
    }

    @Override
    protected void fromMcBufferInternal(PacketByteBuf buffer) {
        super.fromMcBufferInternal(buffer);
        int argb = buffer.readInt();
        red = ((argb >> 16) & 0xFF) / 255.0f;
        green = ((argb >> 8) & 0xFF) / 255.0f;
        blue = ((argb >> 0) & 0xFF) / 255.0f;
        alpha = ((argb >>> 24) & 0xFF) / 255.0f;
    }

    @Override
    protected void toMcBufferInternal(PacketByteBuf buffer) {
        super.toMcBufferInternal(buffer);
        buffer.writeInt(asArgb());
    }

    @Override
    public ColouredFluidKey getFluidKey() {
        return (ColouredFluidKey) super.getFluidKey();
    }

    @Override
    public Text getName() {
        MutableText copy = super.getName().copyContentOnly();
        float r = getRed();
        float g = getGreen();
        float b = getBlue();
        float a = getAlpha();
        if (a < 1) {
            // Rescale towards white, rather than black
            r = 1 - ((1 - r) * a);
            g = 1 - ((1 - g) * a);
            b = 1 - ((1 - b) * a);
        }

        int rgb = (Math.round(r * 255) << 16 //
            | Math.round(g * 255) << 8 //
            | Math.round(b * 255) << 0);

        copy.setStyle(copy.getStyle().withColor(TextColor.fromRgb(rgb)));
        return copy;
    }

    public Text getNoncolouredName() {
        return super.getName();
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    public float getAlpha() {
        return alpha;
    }

    @Override
    public int getRenderColor() {
        return asArgb();
    }

    public final int asArgb() {
        return Math.round(getRed() * 255) << 16 //
            | Math.round(getGreen() * 255) << 8 //
            | Math.round(getBlue() * 255) << 0 //
            | Math.round(getAlpha() * 255) << 24;
    }

    public void setArgb(int argb) {
        setRgba(
            ((argb >> 16) & 0xFF) / 255f, //
            ((argb >> 8) & 0xFF) / 255f, //
            ((argb >> 0) & 0xFF) / 255f, //
            ((argb >>> 24) & 0xFF) / 255f
        );
    }

    public void setAbgr(int abgr) {
        setRgba(
            ((abgr >> 0) & 0xFF) / 255f, //
            ((abgr >> 8) & 0xFF) / 255f, //
            ((abgr >> 16) & 0xFF) / 255f, //
            ((abgr >>> 24) & 0xFF) / 255f
        );
    }

    /** Changes the red, green, and blue colours, leaving alpha unchanged. */
    public void setRgb(float red, float green, float blue) {
        ColouredFluidKey key = getFluidKey();
        this.red = Math.max(key.minRed, Math.min(key.maxRed, red));
        this.green = Math.max(key.minGreen, Math.min(key.maxGreen, green));
        this.blue = Math.max(key.minBlue, Math.min(key.maxBlue, blue));
    }

    public void setRgba(float red, float green, float blue, float alpha) {
        ColouredFluidKey key = getFluidKey();
        this.red = Math.max(key.minRed, Math.min(key.maxRed, red));
        this.green = Math.max(key.minGreen, Math.min(key.maxGreen, green));
        this.blue = Math.max(key.minBlue, Math.min(key.maxBlue, blue));
        this.alpha = Math.max(key.minAlpha, Math.min(key.maxAlpha, alpha));
    }

    @Override
    protected ColouredFluidVolume copy0() {
        ColouredFluidVolume copy = (ColouredFluidVolume) super.copy0();
        copy.red = red;
        copy.green = green;
        copy.blue = blue;
        copy.alpha = alpha;
        return copy;
    }

    @Override
    protected FluidVolume split0(FluidAmount toTake, RoundingMode rounding) {
        ColouredFluidVolume split = (ColouredFluidVolume) super.split0(toTake, rounding);
        split.red = red;
        split.green = green;
        split.blue = blue;
        split.alpha = alpha;
        return split;
    }

    @Override
    protected void mergeInternal(FluidVolume other, FluidMergeResult mergedAmounts) {
        double amount0 = getAmount_F().asInexactDouble();
        double amount1 = other.getAmount_F().asInexactDouble();

        super.mergeInternal(other, mergedAmounts);

        amount1 -= other.getAmount_F().asInexactDouble();
        double total = amount0 + amount1;

        amount0 /= total;
        amount1 /= total;

        ColouredFluidVolume o = (ColouredFluidVolume) other;

        double r0 = red * red;
        double g0 = green * green;
        double b0 = blue * blue;
        double a0 = alpha * alpha;

        double r1 = o.red * o.red;
        double g1 = o.green * o.green;
        double b1 = o.blue * o.blue;
        double a1 = o.alpha * o.alpha;

        double rt = r0 * amount0 + r1 * amount1;
        double gt = g0 * amount0 + g1 * amount1;
        double bt = b0 * amount0 + b1 * amount1;
        double at = a0 * amount0 + a1 * amount1;

        setRgba((float) Math.sqrt(rt), (float) Math.sqrt(gt), (float) Math.sqrt(bt), (float) Math.sqrt(at));
    }

    @Override
    public void addTooltipExtras(FluidTooltipContext context, List<Text> tooltip) {
        super.addTooltipExtras(context, tooltip);
        if (context.isAdvanced()) {
            tooltip.add(Text.literal("ARGB = 0x" + Integer.toHexString(asArgb())));
            tooltip.add(Text.literal("red = " + red));
            tooltip.add(Text.literal("green = " + green));
            tooltip.add(Text.literal("blue = " + blue));
            tooltip.add(Text.literal("alpha = " + alpha));
        }
    }
}
