Mineground command: /world
==========

Gives administrators the ability to list, create, remove and manage the worlds available on Mineground. Various advanced settings may be changed as well, although these may require the management permission.

[« back to command overview](../commands.md)

----------
+ **/world list**

  Displays a list of which worlds are available on the Mineground server.

+ **/world set animals** *[limit=15]*

  Sets the maximum amount of animals which may spawn in any chunk in the current world. The limit needs to be between 0 (disables animals altogether) and 150, with the default value being 15.

* **/world set difficulty** *[difficulty=peaceful, easy, normal, hard]*

  Sets the difficulty of the current world. In a peaceful world, the server will not spawn mobs, the food bar will not deplete and health will automatically regenerate. In an easy world, hostile mobs will spawn, but will do significantly less damage. The food bar will deplete, but will keep a minimum of five hearts. In a normal world, hostile mobs spawn and incur normal damage. The food bar will deplete, but will keep a minimum of half a heart. In a hard world, hostile mobs spawn and incur increased damage. The food bar depletes and can kill the player completely. Zombies will break through wooden doors, and spiders can spawn with potion effects.

* **/world set mobs** *[limit=50]*

  Sets the maximum amount of mobs which will spawn in any chunk in the current world. The limit needs to be between 0 (disables mob spawning altogether) and 150, with the default value being 50.

* **/world set pvp** *[enabled=allowed, disallowed, default]*

  Sets whether PVP (player-versus-player) should be enabled for the current world. The value *allowed* means that any player can attack any player, regardless of their own preferences. *disallowed* means that no one will be able to kill no one, whereas *default* means it may be enabled depending on the player's own settings.

* **/world set rule** *[rule]* *[value=true, false]*

  Enables or disables advanced, name-based settings. These are Minecraft's *game rules*, and may change between releases. The following rules are available:

  * **block-drops**: Whether breaking a block should drop items.
  * **command-block-notify**: Notify online operators when a player uses a command block.
  * **daylight-cycle**: Whether the day and night cycles should be enabled. When disabled, it basically locks the sun.
  * **fire-spread**: Whether fire can spread from one block to another.
  * **health-regeneration**: Whether a player's health should be naturally regenerated.
  * **keep-inventory**: Whether players should keep their inventory after dying.
  * **mobs-damage**: Whether mobs can do damage to blocks.
  * **mobs-loot**: Whether mobs should drop items when they're killed.
  * **mobs-spawn**: Whether mobs should be spawned in this world at all.

  Again, please do mind that any of these commands may stop working in future releases of Minecraft.

* **/world set spawn** *here*

  Sets the spawn position of the current world.

----------

Tab completion is available for the entire /world command.

This command is implemented in the commands for the [WorldManager](../../src/main/java/com/mineground/features/WorldCommands.java) feature.