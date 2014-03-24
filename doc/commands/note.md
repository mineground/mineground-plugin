Mineground command: /note
==========

Allows members of the Mineground staff to create a note, and store it with a player's account. This should be used for permanent notations, such as mentioning that a player might be suspected of grieving. Only members of the staff can access the notes, through the [/notes](notes.md) command.

[« back to command overview](../commands.md)

----------
+ **/note** *[player]* *[message]*

  Writes *message* as a note to *player*'s profile. The player does not have to be in-game right now.

----------

This command is accessible from in-game and the console.

This command is aliased as **/addnote**, because players familiar with older Mineground versions may be familiar with that.

This command is implemented in the [AdministratorCommands](../../src/main/java/com/mineground/features/AdministratorCommands.java) feature.