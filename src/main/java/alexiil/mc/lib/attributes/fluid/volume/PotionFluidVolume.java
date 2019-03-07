package alexiil.mc.lib.attributes.fluid.volume;

import java.util.List;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TranslatableTextComponent;

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
    public int getRenderColor() {
        return PotionUtil.getColor(getFluidKey().potion);
    }

    @Override
    public TextComponent getTextComponent() {
        return new TranslatableTextComponent(getPotion().getName("item.minecraft.potion.effect."));
    }

    @Override
    public List<TextComponent> getTooltipText(TooltipContext ctx) {
        List<TextComponent> list = super.getTooltipText(ctx);
        PotionUtil.buildTooltip(PotionUtil.setPotion(new ItemStack(Items.POTION), getPotion()), list, 1.0F);
        return list;
    }
}
