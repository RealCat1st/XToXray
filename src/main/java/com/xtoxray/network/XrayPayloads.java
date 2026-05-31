package com.xtoxray.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class XrayPayloads {
    public static final Identifier HANDSHAKE_ID = Identifier.parse("xtoxray:handshake");
    public static final Identifier HANDSHAKE_ACK_ID = Identifier.parse("xtoxray:handshake_ack");
    public static final Identifier SYNC_VEIN_ID = Identifier.parse("xtoxray:sync_vein");

    public record HandshakeC2S() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakeC2S> TYPE = new CustomPacketPayload.Type<>(HANDSHAKE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeC2S> CODEC = StreamCodec.unit(new HandshakeC2S());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HandshakeS2C() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakeS2C> TYPE = new CustomPacketPayload.Type<>(HANDSHAKE_ACK_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakeS2C> CODEC = StreamCodec.unit(new HandshakeS2C());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncVeinMinerC2S(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncVeinMinerC2S> TYPE = new CustomPacketPayload.Type<>(SYNC_VEIN_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncVeinMinerC2S> CODEC = StreamCodec.of(
            (buf, val) -> buf.writeBoolean(val.active),
            buf -> new SyncVeinMinerC2S(buf.readBoolean())
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
