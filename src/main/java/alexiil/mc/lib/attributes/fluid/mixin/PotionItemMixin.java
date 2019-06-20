package alexiil.mc.lib.attributes.fluid.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;

import alexiil.mc.lib.attributes.fluid.FluidProviderItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(PotionItem.class)
public class PotionItemMixin extends Item implements FluidProviderItem {

    public PotionItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        Potion potion = PotionUtil.getPotion(stack.obj);
        if (potion == Potions.EMPTY) {
            return FluidKeys.EMPTY.withAmount(0);
        }

        FluidKey fluidKey = FluidKeys.get(potion);
        if (fluidKey == null) {
            return FluidKeys.EMPTY.withAmount(0);
        }
        stack.obj = new ItemStack(Items.GLASS_BOTTLE);
        return fluidKey.withAmount(FluidVolume.BOTTLE);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        return false;
    }
}
