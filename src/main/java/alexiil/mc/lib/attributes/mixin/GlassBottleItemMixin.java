package alexiil.mc.lib.attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;

import alexiil.mc.lib.attributes.fluid.IFluidItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.PotionFluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(GlassBottleItem.class)
public class GlassBottleItemMixin extends Item implements IFluidItem {

    public GlassBottleItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        return FluidKeys.EMPTY.withAmount(0);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        if (with.obj instanceof PotionFluidVolume) {
            PotionFluidVolume potionFluid = (PotionFluidVolume) with.obj;
            Potion potion = potionFluid.getPotion();
            with.obj = with.obj.copy();
            FluidVolume split = with.obj.split(FluidVolume.BOTTLE);
            if (!split.isEmpty()) {
                ItemStack potionStack = new ItemStack(Items.POTION);
                PotionUtil.setPotion(potionStack, potion);
                stack.obj = potionStack;
                return true;
            }
        }
        return false;
    }
}
