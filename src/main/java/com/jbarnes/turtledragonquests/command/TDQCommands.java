package com.jbarnes.turtledragonquests.command;

import com.jbarnes.turtledragonquests.currency.CurrencyService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TDQCommands {
    private TDQCommands() {}

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tdq")
                .then(Commands.literal("balance").executes(TDQCommands::balance))
                .then(Commands.literal("deposit").executes(TDQCommands::deposit))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 64))
                                .executes(TDQCommands::withdraw)))
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
}
