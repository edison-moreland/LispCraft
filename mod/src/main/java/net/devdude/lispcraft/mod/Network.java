package net.devdude.lispcraft.mod;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.network.ServerAccess;

public class Network {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(Mod.id("channel"));

    public static void initialize() {
        CHANNEL.registerServerbound(PingPacket.class, Network::handlePing);
    }

    public static void sendPing(String ping) {
        CHANNEL.clientHandle().send(new PingPacket(ping));
    }

    public static void handlePing(PingPacket packet, ServerAccess access) {
        Mod.LOGGER.info("Ping {}", packet.ping);
    }

    public record PingPacket(String ping) {
    }
}
