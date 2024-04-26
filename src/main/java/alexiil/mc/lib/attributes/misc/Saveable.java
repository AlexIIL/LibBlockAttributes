/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

/** An object that can be saved and loaded (in place, unlike fluid keys or volumes which must be loaded from a
 * specialised static method). */
public interface Saveable {
    default NbtCompound toTag(RegistryWrapper.WrapperLookup lookup) {
        NbtCompound tag = new NbtCompound();
        toTag(tag, lookup);
        return tag;
    }

    void toTag(NbtCompound tag, RegistryWrapper.WrapperLookup lookup);

    void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup lookup);
}
