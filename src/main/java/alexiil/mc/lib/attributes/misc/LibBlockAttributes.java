/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.Identifier;

public class LibBlockAttributes {
    private static final String MODID = "libblockattributes";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }
}
