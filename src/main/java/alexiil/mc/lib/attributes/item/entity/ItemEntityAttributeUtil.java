/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
