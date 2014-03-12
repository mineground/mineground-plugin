Mineground command: /home
==========

The /home command allows players to save the location of their home, and quickly teleport back to it. The primary difference with the [/warp](warp.md) command is that whereas /warp is restricted to the player's current world, /home can teleport the player across worlds.

[« back to command overview](../commands.md)

----------
+ **/home**

  Teleports you to your home location. The teleportation will happen regardless of the world you and your house are in, unless access to the world has been restricted by a Management member.
  
+ **/home set**

  Updates the location of home to the very location where this command is executed.

----------

This command is implemented in the [LocationManager](../../src/main/java/com/mineground/features/LocationManager.java) feature.