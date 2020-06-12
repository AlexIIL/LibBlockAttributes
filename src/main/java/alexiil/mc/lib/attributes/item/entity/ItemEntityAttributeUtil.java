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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
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

    /** @return An {@link ItemInsertable} that spawns {@link ItemEntity}s at the given position. */
    public static ItemInsertable createItemEntityDropper(World world, BlockPos pos) {
        return createItemEntityDropper(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** @return An {@link ItemInsertable} that spawns {@link ItemEntity}s at the given position. */
    public static ItemInsertable createItemEntityDropper(World world, Vec3d vec) {
        return createItemEntityDropper(world, vec.getX(), vec.getY(), vec.getZ());
    }

    /** @return An {@link ItemInsertable} that spawns {@link ItemEntity}s at the given position. */
    public static ItemInsertable createItemEntityDropper(World world, double x, double y, double z) {
        return (stack, simulation) -> {
            if (simulation.isAction()) {
                world.spawnEntity(new ItemEntity(world, x, y, z, stack.copy()));
            }
            return ItemStack.EMPTY;
        };
    }

    /** @return An {@link ItemInsertable} that scatters {@link ItemEntity}s at the given position using
     *         {@link ItemScatterer#spawn(World, double, double, double, net.minecraft.item.ItemStack)} */
    public static ItemInsertable createItemEntityScatterer(World world, BlockPos pos) {
        return createItemEntityScatterer(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** @return An {@link ItemInsertable} that scatters {@link ItemEntity}s at the given position using
     *         {@link ItemScatterer#spawn(World, double, double, double, net.minecraft.item.ItemStack)} */
    public static ItemInsertable createItemEntityScatterer(World world, Vec3d vec) {
        return createItemEntityScatterer(world, vec.getX(), vec.getY(), vec.getZ());
    }

    /** @return An {@link ItemInsertable} that scatters {@link ItemEntity}s at the given position using
     *         {@link ItemScatterer#spawn(World, double, double, double, net.minecraft.item.ItemStack)} */
    public static ItemInsertable createItemEntityScatterer(World world, double x, double y, double z) {
        return (stack, simulation) -> {
            if (simulation.isAction()) {
                ItemScatterer.spawn(world, x, y, z, stack);
            }
            return ItemStack.EMPTY;
        };
    }
}
