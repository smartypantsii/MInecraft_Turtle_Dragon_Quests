# Agent notes — Turtle Dragon Quests

Minecraft **NeoForge 1.21.1** mod. Full design: [`docs/DESIGN.md`](docs/DESIGN.md).

## Loader and pins

- Minecraft `1.21.1`, NeoForge `21.1.259`, toml range **`[21.1.259,21.2)`**
- Java **21**, Gradle **9.2.1**, ModDevGradle **`net.neoforged.moddev` 2.0.148**
- Parchment MC `1.21.1` / mappings `2024.11.17`
- Mod version `0.1.0`, network protocol `"1"`
- License: **LGPL-3.0-only** (`mod_license` / toml `license`)
- Display name: **Turtle Dragon Quests**
- Currency: **TDG Coin** (`turtle_dragon_quests:tdg_coin`). System Shop prices are TDG only.

**Never copy** `/home/jbarnes/projects/bigger-chickens` (Fabric Loom, Java 25, MC 1.21.11).

**Never use** NeoForge 26.x APIs (`SavedDataType` codec constructors, Java 25 toolchain). 1.21.1 `SavedData` is NBT `Factory` + `computeIfAbsent` on overworld `DimensionDataStorage`.

Do not jump NeoForge to 21.2+. Prefer `[21.1.259,21.2)` over `[21.1.259,)`.

## Ids

- Mod id: `turtle_dragon_quests`
- Package: `com.jbarnes.turtledragonquests`
- Helper: `TDQ.id(path)` → `ResourceLocation.fromNamespaceAndPath(MOD_ID, path)`
- Do not use Yarn `Identifier` as the public type.

## Layout

```
com.jbarnes.turtledragonquests
├── TurtleDragonQuests.java   // @Mod
├── TDQ.java
├── registry/
├── currency/                 // WalletAttachment, CurrencyService, AchievementReward
├── quest/
├── shop/
├── network/c2s|s2c/
├── command/
├── event/
├── client/                   // client-only; dedicated server must not classload screens
└── datagen/
```

Client screen classes live under `client/` and are referenced from play-to-client handlers so the dedicated server does not initialize them at static init.

## Event buses

- **Mod bus:** deferred registers, `DataPackRegistryEvent.NewRegistry`, `RegisterPayloadHandlersEvent`, `RegisterMenuScreensEvent`, `GatherDataEvent`, `RegisterKeyMappingsEvent`
- **`NeoForge.EVENT_BUS`:** commands, gameplay, login/clone/dimension, `OnDatapackSyncEvent`, `AdvancementEvent.AdvancementEarnEvent`

## Persistence

- Wallet + quest progress: player `AttachmentType` + codec + **`copyOnDeath()`** after `serialize`. Wallet holds `tdg`, `starterGranted`, `paidAchievements`.
- Shop remaining stock: overworld `SavedData.Factory` file `turtle_dragon_quests_shop.dat`
- Definitions: synced datapack registries for quests, shop offers, and **achievement TDG maps**. Quests are not vanilla advancements.

## Frozen pack JSON

Snake_case fields. **Not** vanilla `ItemStack.CODEC` (`id` / `components`).

- `SimpleStack`: `{ "item": "minecraft:iron_ingot", "count": 2 }` (`count` optional, default 1, range 1–64)
- `ItemIngredient`: `item` XOR `tag`. Tag is `"minecraft:logs"` with **no `#`**. Use `TagKey.codec(Registries.ITEM)`, **not** `hashedCodec`. Same for kill `entity_tag`.
- Visit `box`: `{ "min": [x,y,z], "max": [x,y,z] }` via a dedicated `{min,max}` `BlockPos` codec. **Not** vanilla `BlockBox.CODEC` (6-int array).
- Objectives/rewards: `Codec.dispatch` + `MapCodec` (not `dispatchedCodec`)
- No `repeatable` / cooldown fields in v1

### Datapack path (do not “fix”)

Registry `turtle_dragon_quests:quest` loads from:

`data/<namespace>/turtle_dragon_quests/quest/<path>.json`

The doubled `turtle_dragon_quests` is datapack namespace + registry namespace. Same for `shop_offer` and `achievement_reward`. Built-in files:

`src/generated/resources/data/turtle_dragon_quests/turtle_dragon_quests/{quest,shop_offer,achievement_reward}/`

Commit generated JSON. Ignore only `src/generated/**/.cache/`.

## Quests

- Open log **only** after server sync + `OpenQuestLogS2C`. Command, journal `use`, and keybind `J` all go through the server. No local-only open as the primary path.
- Rewards are **manually claimed**. Objectives are AND. Prerequisites unlock on **`CLAIMED`**.
- Quest `currency` JSON field is **`tdg`**, not `coins`. Claim may also grant special **item** / **unlock** rewards.
- Shipped content is the **six-chapter campaign** in `docs/DESIGN.md` (First Light → Dragon). Only `first_timber` is `auto_accept`. Do not ship disconnected auto-accept stubs or a visit at `[0,64,0]`.
- **Gather:** live possess while `ACTIVE` (count can decrease). Latches when status becomes `COMPLETED`. Recompute on pickup, container close, login, open log, claim, and every 20 ticks while any ACTIVE gather exists. Bundles do **not** count in v1.
- Submit consumes items. Kill / craft / visit latch. Kill: `LivingDeathEvent` `LOWEST`, `!isCanceled()`, attacker `instanceof ServerPlayer` (pets do not count).
- `UnlockReward` only affects the quest graph, not shop `required_quests`.

## TDG Coin

- Only shop currency. Item id `turtle_dragon_quests:tdg_coin`. Display name **TDG Coin**.
- Earn: system quest `currency` rewards on claim; mapped vanilla advancements (`achievement_reward` registry); System Shop sell; admin commands.
- Do **not** credit TDG from generic mining/kills/crafts unless a quest or achievement mapping says so.
- Achievement grants are **pay-once** (`paidAchievements`). Login and `/reload` backfill newly mapped, already-earned advancements. Revoke+re-earn does not pay again.
- No custom achievement UI in v1.

## System Shop

- Player-facing name **System Shop**. Catalog item: System Shop Catalog.
- `ShopMenu`: slots 0–8 sell inputs + player inventory. No phantom buy output.
- Economy clicks: **`clickMenuButton(offerIndex)` only** (1 unit). No Buy/Sell C2S price packets. No C2S `ItemStack`s.
- Extra data: `RegistryFriendlyByteBuf` via `player.openMenu(provider, buf -> …)` / `IMenuProviderExtension.writeClientSideData`.
- `global_stock` = remaining **units/transactions**, lazy-seeded. `/reload` never raises remaining above JSON and never resurrects a depleted key. Admin `/tdq shop stock` restocks.
- Reject spectators and dead players. `unit_price < 0` is a codec error; `0` is a free sample.
- Validate fully (funds, space, stock), then apply. Use `Math.multiplyExact` for `unitPrice * units`.

## Networking

- `CustomPacketPayload` + `StreamCodec` + `(payload, IPayloadContext)`
- Registrar version `"1"`; grow with trailing optional fields
- Clients send ids/counts only
- Single `ActionResultS2C(ok, Component reason)`
- Client caches (`ClientQuestData`, `ClientWallet`, `ClientShopState`) are S2C-only
- `/reload` fan-out: `OnDatapackSyncEvent` (`getPlayer() == null` on broadcast). Recompute graphs, sync all players, close open `ShopMenu`s. `/tdq reload` only calls `server.reloadResources`; it does not duplicate loading.

## Services

Final classes with static methods (`CurrencyService`, `QuestEngine`, `ShopService`). Not interfaces.

## Verify

- PR 1 gate: **`./gradlew build` only**
- Do **not** gate PR 1 on `runClient` or empty `runGameTestServer` (MDK crashes with zero tests)
- GameTests start with currency / kill / shop in later PRs
- Logout persistence and LAN wallet isolation are **manual** checks

## Non-goals (v1)

No mixins, custom mobs, dimensions, shop block, custom achievement UI, FTB Quests, economy SPIs, repeatable quests, OR-groups, or ports off 1.21.1 NeoForge.
