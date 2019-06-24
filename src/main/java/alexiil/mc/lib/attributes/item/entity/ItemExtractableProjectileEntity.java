/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.entity;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class ItemExtractableProjectileEntity implements ItemExtractable {
    private final ProjectileEntity entity;

    public ItemExtractableProjectileEntity(ProjectileEntity entity) {
        this.entity = entity;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        if (!entity.isAlive()) {
            return ItemStack.EMPTY;
        }

        // TODO: Getter for ProjectileEntity.asItemStack

        return ItemStack.EMPTY;
    }
}
