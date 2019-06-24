/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

/** Used in various places for testing [...] */
// TODO: grammar!
public enum Simulation {
    // TODO: Better names!
    // for all 3 here...
    SIMULATE,
    ACTION;

    public boolean isSimulate() {
        return this == SIMULATE;
    }

    public boolean isAction() {
        return this == ACTION;
    }
}
