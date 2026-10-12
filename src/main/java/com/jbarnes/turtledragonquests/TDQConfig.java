package com.jbarnes.turtledragonquests;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TDQConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue STARTING_COINS = BUILDER
            .comment("One-time TDG grant on first login.")
            .defineInRange("starting_coins", 0L, 0L, 10_000_000_000L);

    public static final ModConfigSpec.LongValue MAX_COINS = BUILDER
            .comment("Wallet credit cap.")
            .defineInRange("max_coins", 10_000_000_000L, 1L, 10_000_000_000L);

    public static final ModConfigSpec.BooleanValue ALLOW_COIN_ITEM = BUILDER
            .comment("Allow withdraw/deposit of the TDG Coin item.")
            .define("allow_coin_item", true);

    public static final ModConfigSpec.IntValue SHOP_ACTION_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between System Shop buy/sell clicks.")
            .defineInRange("shop_action_cooldown_ticks", 5, 0, 200);

    public static final ModConfigSpec.IntValue VISIT_CHECK_INTERVAL_TICKS = BUILDER
            .comment("Interval for visit checks and gather recomputes.")
            .defineInRange("visit_check_interval_ticks", 20, 1, 200);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TDQConfig() {}

    public static long startingCoins() {
        return STARTING_COINS.get();
    }

    public static long maxCoins() {
        return MAX_COINS.get();
    }

    public static boolean allowCoinItem() {
        return ALLOW_COIN_ITEM.get();
    }

    public static int shopActionCooldownTicks() {
        return SHOP_ACTION_COOLDOWN_TICKS.get();
    }

    public static int visitCheckIntervalTicks() {
        return VISIT_CHECK_INTERVAL_TICKS.get();
    }
}
