Mineground command: /warp
==========

Players have the ability to save locations in the world for the purpose of being able to teleport back there later on. Saved locations are only available from the world they exist in, and must have a unique name for that world. It's possible to protect a warp using a password as well, if the location contains valuables.

[« back to command overview](../commands.md)

----------
+ **/warp** *[name]* *[password]*

  Teleports to a saved location identified by **name**. You can teleport to locations saved by any player, but may be asked to enter a password if they protected it. Staff members can teleport to any saved location without the password.

+ **/warp create** *[name]* *[password]*

  Creates a new warp point in the current world identified by **name**. While you will own the warp, every other player can teleport to it as well. You can optionally enter a password for protecting the location.
  
+ **/warp list**

  Shows a list of the locations you have saved in the world you're currently in.
  
+ **/warp remove** *[name]*

  Removes the saved location, indicated by **name**, from the world. No one will be able to teleport to it anymore, but any player will be able to create a new warp with this name again. You can only remove your own saved locations, although staff members can override this restriction.

----------

Tab completion is available for the sub-commands of /warp. Implementing completion of warp names is tracked in [issue 1](https://github.com/mineground/mineground-plugin/issues/1).

This command is implemented in the commands for the [LocationManager](../../src/main/java/com/mineground/features/LocationCommands.java) feature.
