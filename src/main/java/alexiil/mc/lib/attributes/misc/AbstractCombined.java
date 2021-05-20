/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.misc;

import java.util.List;

/** Implementation helper class for LBA {@link Combined}. */
public abstract class AbstractCombined<T> implements Combined {

    protected final List<? extends T> list;

    protected AbstractCombined(List<? extends T> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) {
            return false;
        }
        return list.equals(((AbstractCombined<?>) obj).list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public int getSubObjectCount() {
        return list.size();
    }

    @Override
    public Object getSubObject(int index) {
        return list.get(index);
    }

    @Override
    public String toString() {
        if (list.isEmpty()) {
            return getClass().getSimpleName() + "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        for (Object o : list) {
            sb.append("  ");
            sb.append(o);
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
