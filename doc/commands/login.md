Mineground command: /login
==========

The /login command enables players to identify with their Mineground accounts. Because Mineground is an *offline* server, Mineground.net accounts cannot be used for authentication. Mineground will ask a player to enter their password if their last session was more than 24 hours ago.

[« back to command overview](../commands.md)

----------
+ **/login** *[password]*

  Attempts to log in to your account using your password. You are allowed to try to log in three times before Mineground will automatically kick you. If you lost your password, please [contact an administrator](http://mineground.com/contact-us/)!
  
----------

This command is implemented in the [AccountManager](../../src/main/java/com/mineground/AccountManager.java).