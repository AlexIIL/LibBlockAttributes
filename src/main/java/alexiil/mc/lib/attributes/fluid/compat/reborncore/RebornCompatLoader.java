/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.compat.reborncore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

public final class RebornCompatLoader {
    private RebornCompatLoader() {}

    public static void load() {
        try {
            check();
            LibBlockAttributes.LOGGER.info("RebornCore found, loading compatibility for fluids.");
            RebornFluidCompat.load();
        } catch (ClassNotFoundException cnfe) {
            LibBlockAttributes.LOGGER
                .info("RebornCore not found, not loading compatibility for fluids. (" + cnfe.getMessage() + ")");
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            LibBlockAttributes.LOGGER.info(
                "A different version of RebornCore was found, not loading compatibility for fluids. (" + e.getMessage()
                    + ")"
            );
        } catch (ReflectiveOperationException roe) {
            LibBlockAttributes.LOGGER
                .warn("A different version of RebornCore was found, not loading compatibility for fluids.", roe);
        }
    }

    private static void check() throws ReflectiveOperationException {
        Class<?> machineBaseBlockEntity = c("reborncore.common.blockentity.MachineBaseBlockEntity");
        Class<?> tank = c("reborncore.common.util.Tank");
        Class<?> fluidCfg = c("reborncore.common.blockentity.FluidConfiguration");
        Class<?> fluidValue = c("reborncore.common.fluid.FluidValue");
        Class<?> fluidInstance = c("reborncore.common.fluid.container.FluidInstance");
        Class<?> fluidConfig = c("reborncore.common.blockentity.FluidConfiguration$FluidConfig");
        Class<?> extractConfig = c("reborncore.common.blockentity.FluidConfiguration$ExtractConfig");

        requireMethod(machineBaseBlockEntity, "getTank", new Class[0], tank);
        if (hasOldMethod(machineBaseBlockEntity, "fluidTransferAmount", new Class[0], int.class)) {
            throw new NoSuchMethodException(
                "Found the old method 'reborncore.common.blockentity.MachineBaseBlockEntity.fluidTransferAmount()' "
                    + "returning an int - please update RebornCore to a newer version to get compatibility!"
            );
        }
        requireMethod(machineBaseBlockEntity, "fluidTransferAmount", new Class[0], fluidValue);
        requireField(machineBaseBlockEntity, "fluidConfiguration", fluidCfg);
        requireMethod(tank, "getCapacity", new Class[0], fluidValue);
        requireMethod(fluidValue, "getRawValue", new Class[0], int.class);
        requireMethod(fluidValue, "fromRaw", new Class[] { int.class }, fluidValue);
        requireMethod(tank, "getFluidInstance", new Class[0], fluidInstance);
        requireMethod(tank, "getFluid", new Class[0], Fluid.class);
        requireMethod(tank, "setFluid", new Class[] { Direction.class, fluidInstance }, void.class);
        requireMethod(fluidInstance, "getTag", new Class[0], CompoundTag.class);
        requireMethod(fluidInstance, "setTag", new Class[] { CompoundTag.class }, void.class);
        requireMethod(fluidInstance, "getFluid", new Class[0], Fluid.class);
        requireMethod(fluidInstance, "addAmount", new Class[] { fluidValue }, fluidInstance);
        requireMethod(fluidInstance, "subtractAmount", new Class[] { fluidValue }, fluidInstance);
        requireMethod(fluidCfg, "getSideDetail", new Class[] { Direction.class }, fluidConfig);
        requireMethod(fluidConfig, "getIoConfig", new Class[0], extractConfig);
        requireMethod(extractConfig, "isExtact", new Class[0], boolean.class);
        requireMethod(extractConfig, "isInsert", new Class[0], boolean.class);
        requireMethod(extractConfig, "isEnabled", new Class[0], boolean.class);
    }

    private static Class<?> c(String name) throws ClassNotFoundException {
        return Class.forName(name, false, RebornCompatLoader.class.getClassLoader());
    }

    private static void requireField(Class<?> cls, String name, Class<?> type) throws ReflectiveOperationException {
        Field f = cls.getField(name);
        if (!type.equals(f.getType())) {
            throw new NoSuchFieldException("Needed the field " + f + " to be of type " + type);
        }
    }

    private static void requireMethod(Class<?> cls, String name, Class<?>[] args, Class<?> ret)
        throws ReflectiveOperationException {

        Method m = cls.getMethod(name, args);
        if (!ret.equals(m.getReturnType())) {
            throw new NoSuchMethodException("Needed the method " + m + " to return " + ret);
        }
    }

    private static boolean hasOldMethod(Class<?> cls, String name, Class<?>[] args, Class<?> ret) {
        try {
            Method m = cls.getMethod(name, args);
            return ret.equals(m.getReturnType());
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
