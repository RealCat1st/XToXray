package com.xtoxray;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xtoxray.network.XrayPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XtoXray implements ModInitializer {
    public static final String MOD_ID = "xtoxray";
    public static final Logger LOGGER = LoggerFactory.getLogger("xtoxray");
    public static final String VERSION = FabricLoader.getInstance()
        .getModContainer("xtoxray")
        .map(c -> c.getMetadata().getVersion().getFriendlyString())
        .orElse("unknown");

    public void onInitialize() {
        LOGGER.info("XtoXray {} initialized!", VERSION);

        XrayState.getInstance().load();
        XrayVeinMiner.register();

        PayloadTypeRegistry.serverboundPlay().register(XrayPayloads.HandshakeC2S.TYPE, XrayPayloads.HandshakeC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(XrayPayloads.HandshakeS2C.TYPE, XrayPayloads.HandshakeS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(XrayPayloads.SyncVeinMinerC2S.TYPE, XrayPayloads.SyncVeinMinerC2S.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(XrayPayloads.HandshakeC2S.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() != null) {
                    ServerPlayNetworking.send(context.player(), new XrayPayloads.HandshakeS2C());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(XrayPayloads.SyncVeinMinerC2S.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                XrayState.getInstance().setVeinMiner(payload.active());
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("xtoxray")
                .then(Commands.literal("durability")
                    .then(Commands.literal("xblock")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                            .executes(context -> {
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                XrayState.getInstance().setVeinMinerDurability(amount);
                                context.getSource().sendSuccess(() -> Component.literal("Durability cost per block set to " + amount), true);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("veinminer")
                    .then(Commands.literal("on")
                        .executes(context -> {
                            XrayState.getInstance().setVeinMiner(true);
                            context.getSource().sendSuccess(() -> Component.literal("Vein miner enabled"), true);
                            return 1;
                        })
                    )
                    .then(Commands.literal("off")
                        .executes(context -> {
                            XrayState.getInstance().setVeinMiner(false);
                            context.getSource().sendSuccess(() -> Component.literal("Vein miner disabled"), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("status")
                    .executes(context -> {
                        XrayState state = XrayState.getInstance();
                        context.getSource().sendSuccess(() -> Component.literal("Vein miner: " + (state.isVeinMiner() ? "ON" : "OFF") + ", durability per block: " + state.getVeinMinerDurability()), false);
                        return 1;
                    })
                )
            );
        });
    }
}
