/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.init;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;

public class ClientFluidInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LbaFluidProxy.MC_TOOLTIPS_ADVANCED = () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                return false;
            }
            return mc.options == null ? false : mc.options.advancedItemTooltips;
        };
    }
}
