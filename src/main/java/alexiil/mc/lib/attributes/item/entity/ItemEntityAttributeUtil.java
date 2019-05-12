package alexiil.mc.lib.attributes.item.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.impl.EmptyItemExtractable;

public final class ItemEntityAttributeUtil {
    private ItemEntityAttributeUtil() {}

    /** @return An {@link ItemExtractable} for the given entity if it needs special handling for item pickup. (Such as
     *         {@link ItemEntity} or {@link ProjectileEntity}). */
    public static ItemExtractable getSpecialExtractable(Entity entity) {
        if (entity instanceof ItemEntity) {
            return new ItemTransferableItemEntity((ItemEntity) entity);
        } else if (entity instanceof ProjectileEntity) {
            // TODO!
            // return new ItemTransferableArrowEntity((ProjectileEntity) entity);
        }
        return EmptyItemExtractable.NULL;
    }
}
