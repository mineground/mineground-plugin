Mineground command: /weather
==========

Allows VIP members to change the weather of the world they're currently in. When this command is being used from the console or from IRC, the weather will be changed in the default and creative worlds.

[« back to command overview](../commands.md)

----------
+ **/weather sun**

  Changes the world's current weather to be sunny again, for at least 10 minutes.

+ **/weather rain**

  Causes it to rain in the world, for at least 10 minutes. When the temperature in a region is below a certain threshold, or your altitude is above a certain height, it will snow instead.

+ **/weather storm**

  Causes it to rain and thunder in the world. The bad weather will last for at least 10 minutes, but the thunder will end after three minutes. Because thunder can break buildings or set things on fire, this option is only available to staff members.

----------

This command is accessible from in-game and the console.

This command is implemented as a [general command](../../src/main/java/com/mineground/features/GeneralCommands.java).