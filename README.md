# Turtle Dragon Quests

A **NeoForge** mod for Minecraft **1.21.1**. Milestone 1 adds **system quests**, a **System Shop**, and **TDG Coin** as the shop currency.

This is **not** a Fabric mod and **not** Minecraft 1.21.11. Do not copy loader, Gradle, or Java pins from other projects in this workspace.

Design for this milestone: [`docs/DESIGN.md`](docs/DESIGN.md).

## Status

Items, the **TDG wallet**, **quest definitions**, **quest progress**, and all five objective types are in. The **quest log** opens from keybind `J`, `/tdq quests`, or the Quest Journal after a server sync. Achievement TDG and the System Shop land later. Catalog use is still a no-op.

## Requirements (when the skeleton exists)

| Piece | Pin |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.259 (toml range `[21.1.259,21.2)`) |
| Java | 21 |
| Gradle | 9.2.1 (wrapper) |
| Plugin | ModDevGradle `net.neoforged.moddev` 2.0.148 |

## Planned player features

- **System quests** (keybind `J`, `/tdq quests`, Quest Journal): a six-chapter campaign from first logs to the ender dragon. Claiming pays **TDG Coin** and may also grant **special rewards** (items, quest unlocks).
- **System Shop** (keybind `K`, `/tdq shop`, System Shop Catalog): server-wide catalog. Every price is **TDG Coin**. Not villager trades.
- **TDG Coin**: persistent wallet plus physical `tdg_coin` items (withdraw/deposit). Also earned from **mapped vanilla achievements** (paid once).

Objectives in v1: gather (possess), kill, visit, craft, submit.

## Planned commands

Root: `/tdq` (alias `/turtledragon`).

| Command | Who | Effect |
| --- | --- | --- |
| `/tdq quests` | everyone | Open the quest log |
| `/tdq shop` | everyone | Open the System Shop (not spectators) |
| `/tdq balance` | everyone | Print your TDG |
| `/tdq deposit` | everyone | Deposit TDG Coin items |
| `/tdq withdraw <n>` | everyone | Withdraw TDG as items (not spectators) |
| `/tdq reload` | op 2 | Vanilla datapack reload |
| `/tdq currency get/set/add <player> …` | op 2 | Inspect or change wallets |
| `/tdq quest grant/complete/reset <player> …` | op 2 | Force quest state; `reset … all` wipes a player |
| `/tdq shop stock <id> <n>` | op 2 | Set remaining shop units (`0` depletes) |

## Datapacks (planned)

Registry keys use a **doubled** path. That is correct; do not flatten it.

| Registry | JSON path |
| --- | --- |
| `turtle_dragon_quests:quest` | `data/<namespace>/turtle_dragon_quests/quest/<path>.json` |
| `turtle_dragon_quests:shop_offer` | `data/<namespace>/turtle_dragon_quests/shop_offer/<path>.json` |
| `turtle_dragon_quests:achievement_reward` | `data/<namespace>/turtle_dragon_quests/achievement_reward/<path>.json` |

Built-in files live under `data/turtle_dragon_quests/turtle_dragon_quests/…`.

Pack JSON uses `{ "item", "count" }` stacks. Tags are `"minecraft:logs"` with **no** leading `#`. Quest currency rewards use `"tdg"`, not `"coins"`. Vanilla `/reload` and `/tdq reload` both refresh player UIs.

## Config (planned, common spec)

| Key | Default | Meaning |
| --- | --- | --- |
| `starting_coins` | `0` | One-time grant |
| `max_coins` | `10000000000` | Credit cap |
| `allow_coin_item` | `true` | Withdraw/deposit |
| `shop_action_cooldown_ticks` | `5` | Shop click throttle |
| `visit_check_interval_ticks` | `20` | Visit + gather recompute interval |

## Ops wipe

- Player quests: `/tdq quest reset <player> all`
- Player wallet: `/tdq currency set <player> 0`
- System Shop stock: stop the server, delete `<world>/data/turtle_dragon_quests_shop.dat`, restart (stock lazy-reseeds from JSON)

## Build

```bash
./gradlew build
```

The jar will be written to `build/libs/`. Local tools: `./gradlew runClient`, `runServer`, `runData`, `runGameTestServer`.

## Manual checks (when playable)

- `/tdq currency add @s 10` then `/tdq withdraw 10` gives TDG Coin items; `/tdq deposit` returns them to the wallet.
- Logout, dimension change, death, and process restart keep the wallet and quest progress.
- Two LAN clients do not share a wallet.
- `/tdq quest grant @s turtle_dragon_quests:night_watch` then kill three zombies completes the quest; claim pays TDG. A full inventory refuses claim and credits nothing.

## License

[LGPL-3.0-only](https://www.gnu.org/licenses/lgpl-3.0.html).

## Identifiers

- Mod id: `turtle_dragon_quests`
- Package: `com.jbarnes.turtledragonquests`
- Display name: Turtle Dragon Quests
- Currency: TDG Coin (`turtle_dragon_quests:tdg_coin`)
