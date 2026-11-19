package com.dalkco.rightclickplants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings stored in config/right-click-plants.json. */
public final class RightClickPlantsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(RightClickPlantsMod.MOD_ID + ".json");

    private static Data data = new Data();

    public static final class Data {
        /** Right-click a mature crop to harvest it. */
        public boolean enabled = true;
        /** Plant the seed back after harvesting. */
        public boolean replant = true;
        /** Action-bar hint when the seed is in the inventory but not in the hotbar. */
        public boolean hotbarHint = true;
    }

    private RightClickPlantsConfig() {
    }

    public static Data get() {
        return data;
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                data = loaded;
            }
        } catch (Exception e) {
            RightClickPlantsMod.LOGGER.warn("Failed to read {}", FILE, e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            RightClickPlantsMod.LOGGER.warn("Failed to write {}", FILE, e);
        }
    }
}
