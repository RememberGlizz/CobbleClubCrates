package com.cobbleclub.crates.command;

import com.cobbleclub.crates.block.CrateBlock;
import com.cobbleclub.crates.block.ModBlocks;
import com.cobbleclub.crates.block.entity.CrateBlockEntity;
import com.cobbleclub.crates.config.CrateConfigManager;
import com.cobbleclub.crates.config.CrateDefinition;
import com.cobbleclub.crates.economy.ServerEconomy;
import com.cobbleclub.crates.service.CrateService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class CrateCommands {
    private static final SuggestionProvider<ServerCommandSource> CRATE_SUGGESTIONS = (context, builder) -> {
        for (CrateDefinition crate : CrateConfigManager.all()) builder.suggest(crate.id);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> KEY_SUGGESTIONS = (context, builder) -> {
        CrateConfigManager.all().stream()
                .map(crate -> crate.keyId)
                .distinct()
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    private CrateCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("crates")
                .executes(context -> {
                    CrateService.sendMenu(context.getSource().getPlayerOrThrow());
                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .executes(context -> {
                            CrateService.sendPreview(context.getSource().getPlayerOrThrow(), StringArgumentType.getString(context, "crate"));
                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(literal("gems").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            player.sendMessage(Text.literal("Gems: " + ServerEconomy.gems(player))
                    .formatted(Formatting.AQUA, Formatting.BOLD));
            return Command.SINGLE_SUCCESS;
        }));

        dispatcher.register(literal("keys").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            var keys = ServerEconomy.keySnapshot(player);
            if (keys.isEmpty()) {
                player.sendMessage(Text.literal("You do not have any crate keys.").formatted(Formatting.GRAY));
            } else {
                player.sendMessage(Text.literal("Virtual keys:").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                keys.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry ->
                        player.sendMessage(Text.literal(" • " + entry.getKey() + ": " + entry.getValue()).formatted(Formatting.WHITE)));
            }
            return Command.SINGLE_SUCCESS;
        }));

        LiteralArgumentBuilder<ServerCommandSource> admin = literal("ccrates")
                .requires(source -> source.hasPermissionLevel(3));

        admin.then(literal("reload").executes(context -> {
            int count = CrateConfigManager.load();
            feedback(context, "Reloaded " + count + " crate(s).", Formatting.GREEN);
            return Command.SINGLE_SUCCESS;
        }));

        admin.then(literal("place")
                .then(argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .then(argument("style", StringArgumentType.word()).suggests((context, builder) -> {
                            builder.suggest("basic").suggest("vote").suggest("shiny").suggest("legendary");
                            return builder.buildFuture();
                        }).executes(CrateCommands::place))));

        admin.then(literal("remove").executes(CrateCommands::remove));

        admin.then(literal("preview")
                .then(argument("crate", StringArgumentType.word()).suggests(CRATE_SUGGESTIONS)
                        .executes(context -> preview(context, context.getSource().getPlayerOrThrow()))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> preview(context, EntityArgumentType.getPlayer(context, "player"))))));

        admin.then(gemsBranch());
        admin.then(keysBranch());
        dispatcher.register(admin);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> gemsBranch() {
        LiteralArgumentBuilder<ServerCommandSource> branch = literal("gems");
        branch.then(literal("give").then(argument("player", EntityArgumentType.player())
                .then(argument("amount", LongArgumentType.longArg(0L))
                        .executes(context -> changeGems(context, GemOperation.GIVE)))));
        branch.then(literal("take").then(argument("player", EntityArgumentType.player())
                .then(argument("amount", LongArgumentType.longArg(0L))
                        .executes(context -> changeGems(context, GemOperation.TAKE)))));
        branch.then(literal("set").then(argument("player", EntityArgumentType.player())
                .then(argument("amount", LongArgumentType.longArg(0L))
                        .executes(context -> changeGems(context, GemOperation.SET)))));
        return branch;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> keysBranch() {
        LiteralArgumentBuilder<ServerCommandSource> branch = literal("key");
        branch.then(keyOperation("give", KeyOperation.GIVE));
        branch.then(keyOperation("take", KeyOperation.TAKE));
        branch.then(keyOperation("set", KeyOperation.SET));
        return branch;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> keyOperation(String name, KeyOperation operation) {
        return literal(name).then(argument("player", EntityArgumentType.player())
                .then(argument("key", StringArgumentType.word()).suggests(KEY_SUGGESTIONS)
                        .then(argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> changeKeys(context, operation)))));
    }

    private static int place(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String crateId = StringArgumentType.getString(context, "crate").toLowerCase(Locale.ROOT);
        String style = StringArgumentType.getString(context, "style");
        CrateDefinition crate = CrateConfigManager.get(crateId).orElse(null);
        if (crate == null) {
            feedback(context, "Unknown crate: " + crateId, Formatting.RED);
            return 0;
        }

        BlockHitResult hit = target(player);
        if (hit == null) {
            feedback(context, "Look at the block where the crate should be placed.", Formatting.RED);
            return 0;
        }
        BlockPos pos = hit.getBlockPos().offset(hit.getSide());
        Block block = ModBlocks.byStyle(style);
        BlockState state = block.getDefaultState().with(Properties.HORIZONTAL_FACING, player.getHorizontalFacing().getOpposite());
        if (!player.getServerWorld().setBlockState(pos, state, Block.NOTIFY_ALL)) {
            feedback(context, "Could not place a crate there.", Formatting.RED);
            return 0;
        }
        if (player.getServerWorld().getBlockEntity(pos) instanceof CrateBlockEntity blockEntity) {
            blockEntity.configure(crate.id, crate.displayName);
        }
        feedback(context, "Placed " + crate.displayName + " at " + pos.toShortString() + ".", Formatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        BlockHitResult hit = target(player);
        if (hit == null || !(player.getServerWorld().getBlockState(hit.getBlockPos()).getBlock() instanceof CrateBlock)) {
            feedback(context, "Look directly at a CobbleClub crate.", Formatting.RED);
            return 0;
        }
        player.getServerWorld().removeBlock(hit.getBlockPos(), false);
        feedback(context, "Removed the crate.", Formatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int preview(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        CrateService.sendPreview(player, StringArgumentType.getString(context, "crate"));
        return Command.SINGLE_SUCCESS;
    }

    private static int changeGems(CommandContext<ServerCommandSource> context, GemOperation operation)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        long balance = switch (operation) {
            case GIVE -> ServerEconomy.addGems(player, amount);
            case TAKE -> ServerEconomy.addGems(player, -amount);
            case SET -> ServerEconomy.setGems(player, amount);
        };
        feedback(context, player.getName().getString() + " now has " + balance + " Gems.", Formatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int changeKeys(CommandContext<ServerCommandSource> context, KeyOperation operation)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String requestedKey = StringArgumentType.getString(context, "key");
        CrateDefinition crate = CrateConfigManager.all().stream()
                .filter(candidate -> candidate.keyId.equalsIgnoreCase(requestedKey))
                .findFirst()
                .orElse(null);
        if (crate == null) {
            String allowedKeys = CrateConfigManager.all().stream()
                    .map(candidate -> candidate.keyId)
                    .distinct()       .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("none configured");
            feedback(context, "Unknown crate key. Allowed keys: " + allowedKeys, Formatting.RED);
            return 0;
        }
        String keyId = crate.keyId;
        int amount = IntegerArgumentType.getInteger(context, "amount");

        Identifier identifier = Identifier.tryParse(keyId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            feedback(context, "Invalid item identifier for key: " + keyId, Formatting.RED);
            return 0;
        }
        Item keyItem = Registries.ITEM.get(identifier);

        if (operation == KeyOperation.SET) {
            int currentCount = countPhysicalKey(player, keyItem);
            if (amount > currentCount) {
                givePhysicalKey(player, keyItem, amount - currentCount);
            } else if (amount < currentCount) {
                takePhysicalKey(player, keyItem, currentCount - amount);
            }
        } else if (operation == KeyOperation.GIVE) {
            givePhysicalKey(player, keyItem, amount);
        } else if (operation == KeyOperation.TAKE) {
            takePhysicalKey(player, keyItem, amount);
        }

        int finalBalance = countPhysicalKey(player, keyItem);
        feedback(context, player.getName().getString() + " now has " + finalBalance + " " + crate.keyName + "(s).", Formatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int countPhysicalKey(ServerPlayerEntity player, Item keyItem) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(keyItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void givePhysicalKey(ServerPlayerEntity player, Item keyItem, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int toGive = Math.min(remaining, keyItem.getMaxCount());
            ItemStack stack = new ItemStack(keyItem, toGive);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            remaining -= toGive;
        }
    }

    private static void takePhysicalKey(ServerPlayerEntity player, Item keyItem, int amount) {
        int remaining = amount;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(keyItem)) {
                int toTake = Math.min(stack.getCount(), remaining);
                stack.decrement(toTake);
                remaining -= toTake;
            }
        }
    }

    private static BlockHitResult target(ServerPlayerEntity player) {
        HitResult hit = player.raycast(8.0D, 0.0F, false);
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    private static void feedback(CommandContext<ServerCommandSource> context, String message, Formatting color) {
        context.getSource().sendFeedback(() -> Text.literal(message).formatted(color), false);
    }

    private enum GemOperation { GIVE, TAKE, SET }
    private enum KeyOperation { GIVE, TAKE, SET }
}