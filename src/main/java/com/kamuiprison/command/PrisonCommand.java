package com.kamuiprison.command;

import com.kamuiprison.prison.Prisoner;
import com.kamuiprison.prison.PrisonData;
import com.kamuiprison.prison.PrisonManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Comando administrativo (nivel 2):
 *  /kamuiprison list
 *  /kamuiprison free <nome|uuid>   (liberta na posicao de quem executa; no console, no spawn do overworld)
 */
public final class PrisonCommand {
    private PrisonCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kamuiprison")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("free")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        names(ctx.getSource().getServer()), builder))
                                .executes(ctx -> free(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target"))))));
    }

    private static List<String> names(MinecraftServer server) {
        List<String> names = new ArrayList<>();
        for (Prisoner p : PrisonData.get(server).all()) {
            names.add(p.name);
        }
        return names;
    }

    private static int list(CommandSourceStack source) {
        List<Prisoner> all = PrisonData.get(source.getServer()).all();
        if (all.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.kamuiprison.list_empty"), false);
            return 0;
        }
        for (Prisoner p : all) {
            String kind = p.player ? "player" : "mob";
            String line = "[" + kind + "] " + p.name + " (" + p.typeId + ") " + p.id
                    + (p.pendingRelease ? " [pending]" : "");
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return all.size();
    }

    private static int free(CommandSourceStack source, String query) {
        MinecraftServer server = source.getServer();
        PrisonData data = PrisonData.get(server);
        Prisoner match = null;
        for (Prisoner p : data.all()) {
            if (p.id.toString().equalsIgnoreCase(query) || p.name.equalsIgnoreCase(query)) {
                match = p;
                break;
            }
        }
        if (match == null) {
            source.sendFailure(Component.translatable("message.kamuiprison.not_found"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        UUID id = match.id;
        String name = match.name;
        PrisonManager.ReleaseResult result = PrisonManager.releaseAt(server, id, level, pos, Vec3.ZERO,
                source.getRotation().y);
        switch (result) {
            case OK -> source.sendSuccess(() -> Component.translatable("message.kamuiprison.released", name), true);
            case PENDING -> source.sendSuccess(() -> Component.translatable("message.kamuiprison.release_pending", name), true);
            default -> source.sendFailure(Component.translatable("message.kamuiprison.release_failed"));
        }
        return result == PrisonManager.ReleaseResult.FAILED || result == PrisonManager.ReleaseResult.NOT_FOUND ? 0 : 1;
    }
}
