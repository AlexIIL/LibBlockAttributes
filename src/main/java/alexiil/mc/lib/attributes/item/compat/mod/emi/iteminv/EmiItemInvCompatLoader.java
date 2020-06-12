/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.emi.iteminv;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.compat.mod.LbaModCompatLoader;

public final class EmiItemInvCompatLoader extends LbaModCompatLoader {
    private EmiItemInvCompatLoader() {}

    private static final String MOD_NAME = "Emis ItemInventory";

    public static void load() {
        if (!FabricLoader.getInstance().isModLoaded("iteminventory")) {
            return;
        }
        try {
            check();
            LibBlockAttributes.LOGGER.info(MOD_NAME + " found, loading compatibility for items.");
            EmiItemInvCompat.load();
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
        Class<?> iteminv = c("dev.emi.iteminventory.api.ItemInventory");

        requireMethod(iteminv, "getInvSize", new Class[] { ItemStack.class }, int.class);
        requireMethod(iteminv, "getStack", new Class[] { ItemStack.class, int.class }, ItemStack.class);
        requireMethod(iteminv, "setStack", new Class[] { ItemStack.class, int.class, ItemStack.class }, void.class);
        requireMethod(iteminv, "canTake", new Class[] { ItemStack.class, int.class }, boolean.class);
        requireMethod(iteminv, "canInsert", new Class[] { ItemStack.class, int.class, ItemStack.class }, boolean.class);
    }
}
