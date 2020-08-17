/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

import alexiil.mc.lib.attributes.misc.LibBlockAttributes;

/** Stores various options for LBA-fluids. As LBA doesn't ship with a config library these are all stored in
 * config/libblockattributes_fluids.txt, in {@link Properties} format. */
public final class LbaFluidsConfig {
    private static final String FILE_NAME = LibBlockAttributes.MODID_FLUIDS + ".txt";

    /** If true then we should use symbols (B, t) instead of names (Buckets, Ticks). */
    public static final boolean USE_SYMBOLS;

    /** If true the we should use ticks for the time unit instead of seconds. */
    public static final boolean USE_TICKS;

    /** If true the we should use shorthand for localization rather than longhand ("1/4 B" rather than "1 Bucket in a 4
     * Bucket Tank"). */
    public static final boolean USE_SHORT_DESC;

    /** If true then we explicitly remove colours from fluid names. */
    public static final boolean DISABLE_FLUID_COLOURS;

    /** If true then we don't emit white for fluid arguments and gray for everything else. */
    public static final boolean DISABLE_EMPHASIS_COLOURS;

    /** If true then we put the fluid name in the middle of the text, false to put the fluid name at the top. */
    public static final boolean TOOLTIP_JOIN_NAME_AMOUNT;

    static {
        FabricLoader fabric = FabricLoader.getInstance();
        final Path cfgDir;
        if (fabric.getGameDir() == null) {
            // Can happen during a JUnit test
            cfgDir = Paths.get("config");
        } else {
            cfgDir = fabric.getGameDir();
        }
        if (!Files.isDirectory(cfgDir)) {
            try {
                Files.createDirectories(cfgDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create the config directory! (" + cfgDir + ")", e);
            }
        }
        Path cfgFile = cfgDir.resolve(FILE_NAME);
        Properties props = new Properties();
        if (Files.exists(cfgFile)) {
            try (Reader reader = Files.newBufferedReader(cfgFile, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                LibBlockAttributes.LOGGER.error("Failed to read the config file!", e);
            }
        }
        boolean hasAll = true;

        hasAll &= props.containsKey("symbols");
        USE_SYMBOLS = "true".equalsIgnoreCase(props.getProperty("symbols", "false"));

        hasAll &= props.containsKey("ticks");
        USE_TICKS = "true".equalsIgnoreCase(props.getProperty("ticks", "false"));

        hasAll &= props.containsKey("short_desc");
        USE_SHORT_DESC = "true".equalsIgnoreCase(props.getProperty("short_desc", "false"));

        hasAll &= props.containsKey("disable_fluid_colours");
        DISABLE_FLUID_COLOURS = "true".equalsIgnoreCase(props.getProperty("disable_fluid_colours", "false"));

        hasAll &= props.containsKey("disable_emphasis_colours");
        DISABLE_EMPHASIS_COLOURS = "true".equalsIgnoreCase(props.getProperty("disable_emphasis_colours", "false"));

        hasAll &= props.containsKey("tooltip_join_name_amount");
        TOOLTIP_JOIN_NAME_AMOUNT = "true".equalsIgnoreCase(props.getProperty("tooltip_join_name_amount", "false"));

        if (!hasAll) {
            try (Writer fw = Files.newBufferedWriter(cfgFile, StandardCharsets.UTF_8)) {
                fw.append("# LibBlockAttributes options file (fluids module)\n");
                fw.append("# Removing an option will reset it back to the default value\n");
                fw.append("# Removing or altering comments doesn't replace them.\n\n");

                if (!props.containsKey("symbols")) {
                    fw.append("# False to use long names (buckets, ticks) or true to use symbols (B, t)\n");
                    fw.append("symbols=false\n\n");
                }

                if (!props.containsKey("ticks")) {
                    fw.append("# False to use seconds or true to use ticks (1/20th of a second)\n");
                    fw.append("ticks=false\n\n");
                }

                if (!props.containsKey("short_desc")) {
                    fw.append("# False to use long descriptions, or true to use shorter ones. For example:\n");
                    fw.append("#  false: '3 Buckets of Water in a 16 Bucket Tank'\n");
                    fw.append("#  true:  '3 / 16 Buckets of Water'\n");
                    fw.append("short_desc=false\n\n");
                }

                if (!props.containsKey("disable_fluid_colours")) {
                    fw.append("# False to allow fluids to use their own text colour, true to remove it.\n");
                    fw.append("disable_fluid_colours=false\n\n");
                }

                if (!props.containsKey("disable_emphasis_colours")) {
                    fw.append("# True to colour tooltips to emphasise numbers, false to use the default colour.\n");
                    fw.append("disable_emphasis_colours=false\n\n");
                }

                if (!props.containsKey("tooltip_join_name_amount")) {
                    fw.append("# True to join the name with the amount in tooltips. For example:\n");
                    fw.append("# 'true':\n");
                    fw.append("#   '3 Buckets of Water in a 16 Bucket Tank'\n");
                    fw.append("# 'false':\n");
                    fw.append("#   'Water'\n");
                    fw.append("#   '3 Buckets in a 16 Bucket Tank'\n");
                    fw.append("tooltip_join_name_amount=false\n\n");
                }

            } catch (IOException e) {
                LibBlockAttributes.LOGGER.warn("[config] Failed to write the config file!", e);
            }
        }
    }
}
