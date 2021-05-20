/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import net.minecraft.nbt.NbtCompound;

/** An object that can be saved and loaded (in place, unlike fluid keys or volumes which must be loaded from a
 * specialised static method). */
public interface Saveable {
    default NbtCompound toTag() {
        return toTag(new NbtCompound());
    }

    NbtCompound toTag(NbtCompound tag);

    void fromTag(NbtCompound tag);
}
