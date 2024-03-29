/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.transfer;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.compat.mod.LbaModCompatLoader;

public class TransferItemInvCompatLoader extends LbaModCompatLoader {
    private TransferItemInvCompatLoader() {}

    private static final String MOD_NAME = "Fabric Transfer Api v1";

    private static final String PKG = "net.fabricmc.fabric.api.transfer.v1.";

    public static void load() {
        if (!FabricLoader.getInstance().isModLoaded("fabric-transfer-api-v1")) {
            LibBlockAttributes.LOGGER.info(MOD_NAME + " not loaded, not loading compatibility for items");
            return;
        }

        try {
            check();
            LibBlockAttributes.LOGGER.info(MOD_NAME + " found, loading compatibility for items.");
            TransferItemApiCompat.load();
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER
                .info(MOD_NAME + " not found, not loading compatibility for items (" + cnfe.getMessage() + ")");
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            LibBlockAttributes.LOGGER.info(
                "A different version of " + MOD_NAME + " was found, not loading compatibility for items. ("
                    + e.getMessage() + ")"
            );
        } catch (ReflectiveOperationException roe) {
            LibBlockAttributes.LOGGER
                .warn("A different version of " + MOD_NAME + " was found, not loading compatibility for items.", roe);
        }
    }

    private static void check() throws ReflectiveOperationException {
        Class<?> sto = c(PKG + "storage.Storage");
        Class<?> stoView = c(PKG + "storage.StorageView");
        Class<?> itemStoStatic = c(PKG + "item.ItemStorage");
        Class<?> blockApiLookup = c("net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup");
        Class<?> itemVar = c(PKG + "item.ItemVariant");
        Class<?> t = c(PKG + "transaction.Transaction");
        Class<?> tCtx = c(PKG + "transaction.TransactionContext");

        requireField(itemStoStatic, "SIDED", blockApiLookup);
        requireMethod(
            blockApiLookup, "find",
            new Class<?>[] { World.class, BlockPos.class, BlockState.class, BlockEntity.class, Object.class },
            Object.class
        );
        requireMethod(t, "openNested", new Class<?>[] { tCtx }, t);
        requireMethod(t, "getCurrentUnsafe", null, tCtx);
        requireMethod(t, "abort", null, void.class);
        requireMethod(t, "commit", null, void.class);
        requireClassExtends(sto, Iterable.class);
        requireMethod(stoView, "getResource", null, Object.class);
        requireMethod(itemVar, "isBlank", null, boolean.class);
        requireMethod(itemVar, "toStack", null, ItemStack.class);
        requireMethod(stoView, "getAmount", null, long.class);
        requireMethod(stoView, "getCapacity", null, long.class);
        requireMethod(itemVar, "of", new Class<?>[] { ItemStack.class }, itemVar);
        requireMethod(sto, "insert", new Class<?>[] { Object.class, long.class, tCtx }, long.class);
        requireMethod(sto, "extract", new Class<?>[] { Object.class, long.class, tCtx }, long.class);
    }
}
