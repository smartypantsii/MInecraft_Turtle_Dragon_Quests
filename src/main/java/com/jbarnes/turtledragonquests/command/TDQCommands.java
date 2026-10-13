package com.jbarnes.turtledragonquests.command;

import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.jbarnes.turtledragonquests.quest.QuestEngine;
import com.jbarnes.turtledragonquests.registry.TDQDataRegistries;
import com.jbarnes.turtledragonquests.shop.ShopService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TDQCommands {
    private static final SuggestionProvider<CommandSourceStack> QUEST_IDS = (ctx, builder) -> {
        var registry = ctx.getSource().getServer().registryAccess().registryOrThrow(TDQDataRegistries.QUEST_REGISTRY);
        return SharedSuggestionProvider.suggestResource(registry.keySet(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OFFER_IDS = (ctx, builder) -> {
        var registry = ctx.getSource().getServer().registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY);
        return SharedSuggestionProvider.suggestResource(registry.keySet(), builder);
    };

    private TDQCommands() {}

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tdq")
                .then(Commands.literal("quests").executes(TDQCommands::quests))
                .then(Commands.literal("balance").executes(TDQCommands::balance))
                .then(Commands.literal("deposit").executes(TDQCommands::deposit))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 64))
                                .executes(TDQCommands::withdraw)))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(TDQCommands::reload))
                .then(Commands.literal("quest")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                                .suggests(QUEST_IDS)
                                                .executes(TDQCommands::questGrant))))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                                .suggests(QUEST_IDS)
                                                .executes(TDQCommands::questComplete))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("all").executes(TDQCommands::questResetAll))
                                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                                .suggests(QUEST_IDS)
                                                .executes(TDQCommands::questReset)))))
                .then(Commands.literal("shop")
                        .executes(TDQCommands::shop)
                        .then(Commands.literal("stock")
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(OFFER_IDS)
                                        .then(Commands.argument("n", IntegerArgumentType.integer(0))
                                                .executes(TDQCommands::shopStock)))))
                .then(Commands.literal("currency")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(TDQCommands::currencyGet)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("n", LongArgumentType.longArg(0))
                                                .executes(TDQCommands::currencySet))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("n", LongArgumentType.longArg())
                                                .executes(TDQCommands::currencyAdd)))));

        var dispatcher = event.getDispatcher();
        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("turtledragon").redirect(node));
    }

    private static int quests(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestEngine.openLog(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int balance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long tdg = CurrencyService.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turtle_dragon_quests.balance", tdg), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int deposit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long deposited = CurrencyService.depositAll(player);
        if (deposited <= 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.turtle_dragon_quests.deposit.fail", 0));
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.deposit.success", deposited),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int withdraw(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (!CurrencyService.tryWithdraw(player, n)) {
            ctx.getSource().sendFailure(Component.translatable("commands.turtle_dragon_quests.withdraw.fail", n));
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.withdraw.success", n),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int currencyGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        long tdg = CurrencyService.get(target);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.currency.get", target.getGameProfile().getName(), tdg),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int currencySet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        long n = LongArgumentType.getLong(ctx, "n");
        CurrencyService.set(target, n);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.currency.set", target.getGameProfile().getName(), CurrencyService.get(target)),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int currencyAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        long n = LongArgumentType.getLong(ctx, "n");
        if (n > 0) {
            CurrencyService.credit(target, n);
        } else if (n < 0) {
            CurrencyService.tryDebit(target, -n);
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.currency.set", target.getGameProfile().getName(), CurrencyService.get(target)),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turtle_dragon_quests.reload"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int questGrant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        if (!QuestEngine.grant(target, id)) {
            ctx.getSource().sendFailure(Component.translatable("commands.turtle_dragon_quests.quest.missing", id.toString()));
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.quest.grant", target.getGameProfile().getName(), id.toString()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int questComplete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        if (!QuestEngine.forceComplete(target, id)) {
            ctx.getSource().sendFailure(Component.translatable("commands.turtle_dragon_quests.quest.missing", id.toString()));
            return 0;
        }
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.quest.complete", target.getGameProfile().getName(), id.toString()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int questReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        QuestEngine.reset(target, id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.quest.reset", target.getGameProfile().getName(), id.toString()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int questResetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        QuestEngine.resetAll(target);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.quest.reset", target.getGameProfile().getName(), "all"),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int shop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ShopService.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int shopStock(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        int n = IntegerArgumentType.getInteger(ctx, "n");
        MinecraftServer server = ctx.getSource().getServer();
        if (server.registryAccess().registryOrThrow(TDQDataRegistries.SHOP_OFFER_REGISTRY).getOptional(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.turtle_dragon_quests.shop.missing", id.toString()));
            return 0;
        }
        ShopService.setStock(server, id, n);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.turtle_dragon_quests.shop.stock", id.toString(), n),
                true);
        return Command.SINGLE_SUCCESS;
    }
}
