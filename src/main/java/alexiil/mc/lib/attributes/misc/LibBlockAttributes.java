/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import net.minecraft.util.Identifier;

public class LibBlockAttributes {
    private static final String MODID = "libblockattributes";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final String MODID_CORE = "libblockattributes_core";
    public static final String MODID_ITEMS = "libblockattributes_items";
    public static final String MODID_FLUIDS = "libblockattributes_fluids";

    public enum LbaModule {
        ALL(MODID),
        CORE(MODID_CORE),
        ITEMS(MODID_ITEMS),
        FLUIDS(MODID_FLUIDS);

        public final String id;

        private LbaModule(String id) {
            this.id = id;
        }

        public Identifier id(String path) {
            return new Identifier(id, path);
        }

        @Nullable
        public ModContainer getModContainer() {
            return FabricLoader.getInstance().getModContainer(id).orElse(null);
        }
    }

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }
}
