package com.dalkco.rightclickplants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RightClickPlantsMod implements ClientModInitializer {

    public static final String MOD_ID = "right-click-plants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Right Click Plants initialized!");
        RightClickPlantsConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            PendingPlantQueue.tick(client);
        });
    }
}
