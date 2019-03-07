package alexiil.mc.lib.attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.IFluidItem;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.Ref;

@Mixin(BucketItem.class)
public class BucketItemMixin extends Item implements IFluidItem {

    @Shadow
    private Fluid fluid;

    public BucketItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public FluidVolume drain(Ref<ItemStack> stack) {
        if (fluid == Fluids.EMPTY || ((Object) this) instanceof FishBucketItem) {
            return FluidKeys.EMPTY.withAmount(0);
        }

        Item remainder = this.getRecipeRemainder();
        FluidKey fluidKey = FluidKeys.get(fluid);
        if (remainder == null || fluidKey == null) {
            return FluidKeys.EMPTY.withAmount(0);
        }
        stack.obj = new ItemStack(remainder);
        return fluidKey.withAmount(FluidVolume.BUCKET);
    }

    @Override
    public boolean fill(Ref<ItemStack> stack, Ref<FluidVolume> with) {
        if (fluid != Fluids.EMPTY) {
            return false;
        }
        for (Item item : Registry.ITEM) {
            if (item instanceof IFluidItem) {
                IFluidItem bucket = (IFluidItem) item;
                ItemStack newStack = new ItemStack(item);

                Ref<ItemStack> stackRef = new Ref<>(newStack);
                FluidVolume fluidHeld = bucket.drain(stackRef);
                int amount = fluidHeld.getAmount();
                if (FluidVolume.areEqualExceptAmounts(with.obj, fluidHeld) && amount <= with.obj.getAmount()
                    && ItemStack.areEqual(stackRef.obj, stack.obj)) {
                    with.obj = with.obj.copy();
                    assert with.obj.split(amount).getAmount() == amount;
                    stack.obj = newStack;
                    return true;
                }
            }
        }
        return false;
    }
}
