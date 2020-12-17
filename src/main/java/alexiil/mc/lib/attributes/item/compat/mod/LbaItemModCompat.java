/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.compat.mod;

import alexiil.mc.lib.attributes.item.compat.mod.dank.DankItemInvCompatLoader;
import alexiil.mc.lib.attributes.item.compat.mod.emi.iteminv.EmiItemInvCompatLoader;

public final class LbaItemModCompat {
    private LbaItemModCompat() {}

    public static void load() {
        EmiItemInvCompatLoader.load();
        DankItemInvCompatLoader.load();
    }
}
