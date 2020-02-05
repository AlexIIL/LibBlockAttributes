/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fatjar;

/** Internal class - used to store the constant {@link #FATJAR_ERROR}. (Never actually loaded by the vm). */
public final class FatJarChecker {
    private FatJarChecker() {}

    public static final String FATJAR_ERROR = "\n\nLoaded the LBA FatJar outside of a development environment!"
        + "\nThis can cause stability issues when older or newer versions"
        + "\nof the different submodules are present on the classpath, as"
        + "\nfabric loader cannot load the seperate modules correctly. (Which"
        + "\nthen causes NoSuchMethodError's, or other strange behaviour)";
}
