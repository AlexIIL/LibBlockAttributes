package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import alexiil.mc.lib.attributes.fluid.render.DefaultFluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.render.EnchantmentGlintFluidRenderer;
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer;

public final class PotionFluidVolume extends FluidVolume {

    public PotionFluidVolume(PotionFluidKey key, int amount) {
        super(key, amount);
    }

    public PotionFluidVolume(PotionFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    public Potion getPotion() {
        return getFluidKey().potion;
    }

    @Override
    public PotionFluidKey getFluidKey() {
        return (PotionFluidKey) fluidKey;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public List<Component> getTooltipText(TooltipContext ctx) {
        List<Component> list = super.getTooltipText(ctx);
        PotionUtil.buildTooltip(PotionUtil.setPotion(new ItemStack(Items.POTION), getPotion()), list, 1.0F);
        return list;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public FluidVolumeRenderer getRenderer() {
        if (getPotion().getEffects().isEmpty()) {
            return DefaultFluidVolumeRenderer.INSTANCE;
        } else {
            return EnchantmentGlintFluidRenderer.INSTANCE;
        }
    }
}
