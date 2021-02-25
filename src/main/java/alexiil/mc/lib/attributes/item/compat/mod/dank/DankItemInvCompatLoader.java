/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod.dank;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;
import alexiil.mc.lib.attributes.misc.compat.mod.LbaModCompatLoader;

/** Compat for https://github.com/Tfarcenim/DankStorageFabric */
public class DankItemInvCompatLoader extends LbaModCompatLoader {

    private static final String MOD_NAME = "Dank Storage";

    public static void load() {
        try {
            Class<?> dock = c("tfar.dankstorage.blockentity.DockBlockEntity");
            if (!(Inventory.class.isAssignableFrom(dock))) {
                throw new NoSuchMethodException("DockBlockEntity doesn't implement Inventory");
            }
            if (!(BlockEntity.class.isAssignableFrom(dock))) {
                throw new NoSuchMethodException("DockBlockEntity doesn't extend BlockEntity???");
            }

            LibBlockAttributes.LOGGER.info(MOD_NAME + " found, loading compatibility for items.");
            DankItemInvCompat.load((Class<? extends BlockEntity>) dock);
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER
                .info(MOD_NAME + " not found, not loading compatibility for items (" + cnfe.getMessage() + ")");
        } catch (NoSuchMethodException e) {
            LibBlockAttributes.LOGGER.info(
                "A different version of " + MOD_NAME + " was found, not loading compatibility for items. ("
                    + e.getMessage() + ")"
            );
        } catch (ReflectiveOperationException roe) {
            LibBlockAttributes.LOGGER
                .warn("A different version of " + MOD_NAME + " was found, not loading compatibility for items.", roe);
        }
    }
}
