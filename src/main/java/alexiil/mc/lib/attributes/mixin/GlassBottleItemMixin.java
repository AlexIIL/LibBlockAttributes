package alexiil.mc.lib.attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;

import alexiil.mc.lib.attributes.fluid.FluidProviderItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.PotionFluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(GlassBottleItem.class)
public class GlassBottleItemMixin extends Item implements FluidProviderItem {

    public GlassBottleItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        return FluidKeys.EMPTY.withAmount(0);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        if (stack.obj.getAmount() != 1) {
            return false;
        }
        final Potion potion;
        if (with.obj instanceof PotionFluidVolume) {
            potion = ((PotionFluidVolume) with.obj).getPotion();
        } else if (with.obj.fluidKey == FluidKeys.WATER) {
            potion = Potions.WATER;
        } else {
            return false;
        }
        with.obj = with.obj.copy();
        FluidVolume split = with.obj.split(FluidVolume.BOTTLE);
        if (!split.isEmpty()) {
            ItemStack potionStack = new ItemStack(Items.POTION);
            PotionUtil.setPotion(potionStack, potion);
            stack.obj = potionStack;
            return true;
        }
        return false;
    }
}
