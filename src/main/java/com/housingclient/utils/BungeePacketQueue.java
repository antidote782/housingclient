package com.housingclient.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.Packet;

public class BungeePacketQueue {
    /**
     * BungeeCord Timeout Fix Queue
     * Holds Play packets received while the client is still in the Login state.
     */
    public static final Queue<Packet<?>> delayPacketQueue = new ConcurrentLinkedQueue<>();
}
