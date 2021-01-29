# RoleWatcher

This is a Discord bot which can sync roles between a Discord and a forum. That's useful if you offer a Discord server
for the members of a forum that has changing roles due to memberships or other changing permission sets but that does
not grant database access.

## Setup

The following text is written for linux based systems. Windows or Mac systems might need slightly different setup steps.
This bot runs on Java 11, so you need to install the Java 11 JDK for your system.

### Prerequisites

While not specifically needed a basic understanding of computers, databases (SQL) and web traffic will help you a lot.
It might be not that easy otherwise, especially if you need to code the
[Forum role API](#forum-role-api) yourself.

#### Forum IDs

Your forum needs to have user and role IDs that are numeric and start by 1. Your highest possible ID should be 2<sup>
63</sup>-1. If your forum uses so called "Snowflakes" as IDs there might be issues as Discord uses that ID system too.
While duplicates between forum IDs and Discord IDs are very unlikely they can happen if your forum uses them as well. \

#### Forum user name

This program saves forum usernames for better identification. A username has to be given and has to be between 1 and 100
characters long. If your forum has the possibility to set longer usernames you need to limit the usernames you save in
this program. Should your forum not have the option to set usernames you still need to set a username for any user you
save in this program. However, that username does not need to be unique, so you can just set the username to "a" for all
users.

#### Forum role API

This bot needs an API to requests the roles of a user of your forum from. The API needs to have the ability to handle a
users' forum ID as a query parameter and needs to return the IDs of the roles the user has as a JSON encoded array like
this:

```json
[
  1,
  4,
  9
]
```

This answer would show that the user has the roles with the IDs 1, 4 and 9 on the forum. Do not put this array in HTML
or anything, just answer as
`application/json`! \
Any authorization needs to happen via query parameters.

### Tokens & APIs

#### Discord bot token

To use this bot you need a Discord bot token. To create one open the
[Discord Developer Portal](https://discord.com/developers/applications)
and add a new application by clicking on the "New Application" button. \
After creating the application you should see general information about your application. If not please select your
application in the list of applications. Now switch to the "Bot" tab on the left and click the
"Add Bot" button that shows up. \
You should now see information about the bot you created, and a button called "Copy" under a header that says "Token".
Click that button to copy the token of your bot. You will need this token in the
[Environment section](#environment). \
Never give this token to anyone as that is all one needs to control your bot!

#### Discord bot intents

As the bot will need to iterate over all members in your server it needs a permission from Discord. As long as your bot
is not in 100 or more servers you can just enable that option by yourself. To do that open the
[Discord Developer Portal](https://discord.com/developers/applications)
and navigate to the "Bot" tab of your application. Scroll down to the header "Privileged Gateway Intents" and enable
the "Server Members Intent".

#### Adding the bot to your server

Once more in the
[Discord Developer Portal](https://discord.com/developers/applications)
select your application. Now switch to the "OAuth2" tab on the left and in the list of scopes select "bot". Now scroll
down and select all needed permissions:

```text
View Audit Log
Manage Roles
Manage Channels
Kick Members
Ban Members
View Channels
Send Messages
Embed Links
Mention everyone
Use external emojis
Add reactions
```

Back up in the scopes section on that site there is now a link you can use to add the bot to your server with the
selected permissions. To use that link just copy and paste it into a new browser tab or window. \
After performing the steps to add the bot the last thing you need to do is to move the role of the bot (has the same
name as your bot) above all roles that are linked to your forum, so the bot can assign those roles to users. To do that
just navigate to your guild in Discord, open the server settings, select "Roles" and drag-and-drop the bot role above
the highest forum role. \
You can give the bot the administrator permission but keep in mind that it literally means the bot can do anything. That
means if anyone has access to your token to control the bot he can do pretty much anything to your server. However, even
without the administrator permission the bot has enough permissions to interfere a lot with your server so even if you
do not give the bot the administrator permissions you need to keep your token very secure.

#### Additional information about the bot

* The bot can only kick or ban people if the highest role of that user is below the highest role of the bot. It can not
  kick or ban the server owner at all.
* The bot can only assign roles lower than its own.
* The bot can only update permissions of channels it can access.
* Not granting the bot the needed permissions might lead to unknown behaviour.
* Commands can only be used in channels the bot has access to.
* The help command will only show commands your guild can use with the given permissions.

### Environment

The environment variables carry some information for the bot to use. To get your bot running you must create a file
called `.env` in the same location where this file is located and add the following text to it:

```dotenv
WEB_API_OPEN_PORT=

DB_DATABASE=
DB_ROOT_PASSWORD=
DB_USER=
DB_PASSWORD=

DC_TOKEN=

CMD_PREFIX=
EMBED_COLOR_R=
EMBED_COLOR_G=
EMBED_COLOR_B=
OWNER_DISCORD_ID=

BOT_ACTIVITY=
BOT_ACTIVITY_TEXT=
BOT_ACTIVITY_STREAMING_URL=

FORUM_ROLE_API_URL=
FORUM_ROLE_API_DELAY_MS=
FORUM_USER_ADD_API_KEY=
FORUM_MEMBER_PROFILE_URL=
FORUM_BANNED_ROLE_ID=
DEFAULT_AVATAR_URL=

ERROR_REDIRECT_URL=
```

In the following sections each of these settings will be explained. For each of them the following points need to be
met:

* Do not write text in multiple lines after the equal sign (`=`).
* Make sure your lines do not have trailing spaces or tabs!
* While it also works without, you should encapsulate text with spaces in it with quotation marks.

Settings that have to be set are marked with `[REQUIRED]`. If you leave these blank the program will not work correctly
and mostly will not even start completely.

#### WEB_API_OPEN_PORT

This setting defines the port that is opened for incoming traffic and gets used by the web part of the application (the
API to add users). If you do not set a value the default of `8080` will be used. If you use any sort of proxy like nginx
you need to forward the traffic to the specified port. Try to use a port that is not used by other protocols. \
If you use the API to add users keep in mind that the URL needs to include the port if you do not use proxies or port
forwarding, so the URL you need to use might look like one of these:

```text
http://example.com:8080/users?key=123
https://127.0.0.1:8080/users?key=123
```

#### DB_DATABASE

This setting defines the name of the database. If you do not set a value the default name "database" will be chosen. If
you change that value later on and do not rename the files in `/data/` accordingly the program will create a new
database!

#### [REQUIRED] DB_ROOT_PASSWORD

The root password of the database. Make sure to use a secure password!

#### [REQUIRED] DB_USER and DB_PASSWORD

Username and password for the database to make sure no one else can access your database. Make sure to use a secure
password! \
If you change these values after the first run the program will not work as the database got set up with your old
values, so your new credentials are not correct, and the connection will be refused!

#### [REQUIRED] DC_TOKEN

This is the place for the Discord token mentioned in
[Discord bot token](#discord-bot-token). Never share this token with anyone!

#### CMD_PREFIX

This is the prefix the bot needs to react to a command. If this value is set to `?` the bot will only perform the "help"
command if a user writes
`?help`. If no value is set the bot has no prefix, so it only reacts to a message that starts with the actual command
like `help`. Do not use spaces in the prefix!

#### EMBED_COLOR_R

Discord bots can send embedded messages. These messages have a colored line on the left side. That line color is encoded
in RGB so this value represents the **R**ed part of that color. Please use a value between 0-255, otherwise results may
vary. If this value is not set it defaults to 222 which results in an orange color if [EMBED_COLOR_G](#embed_color_g)
and [EMBED_COLOR_B](#embed_color_b) also have no value set.

#### EMBED_COLOR_G

This value represents the **G**reen part of the color. Please use a value between 0-255, otherwise results may vary. If
this value is not set it defaults to 105 which results in an orange color if [EMBED_COLOR_R](#embed_color_r)
and [EMBED_COLOR_B](#embed_color_b) also have no value set.

#### EMBED_COLOR_B

This value represents the **B**lue part of the color. Please use a value between 0-255, otherwise results may vary. If
this value is not set it defaults to 12 which results in an orange color if [EMBED_COLOR_R](#embed_color_r)
and [EMBED_COLOR_G](#embed_color_g) also have no value set.

#### BOT_ACTIVITY

Discord bots can display an activity in the member list. Discord offers a few activities a bot can use which are:

* **listening** (to xyz)
* **playing** (xyz)
* **watching** (xyz)
* **streaming** (xyz)
* **competing** (in xyz)

If you want to display an activity you can use one of the bold printed words. If you use an activity that is not in this
list the activity will default to "**watching** user roles". If you do not want to set an activity just leave this field
blank or remove it completely. \
If you want to use an activity you also need to set a
[BOT_ACTIVITY_TEXT](#bot_activity_text). Otherwise, no activity will be displayed. If you want to use the streaming
activity make sure to also set the
[BOT_ACTIVITY_STREAMING_URL](#bot_activity_streaming_url) as if there is no valid URL set the bot will not display an
activity.

#### BOT_ACTIVITY_TEXT

This value replaces the `xyz` in the list shown in
[BOT_ACTIVITY](#bot_activity) with custom text to further customize your bot activity. A basic example would
be `user roles` which results in
"**watching** user roles" if you also set the fitting activity. If you do not set a text no activity will be shown at
all. Maximum length of this text is 128 characters!

#### BOT_ACTIVITY_STREAMING_URL

A link to a stream, only needs to be set if the streaming activity is used. Has to be valid according to Discord
standards, so it needs to include the "https(s)://" at the start. Discord only supports twitch and YouTube links at the
moment.

#### [REQUIRED] FORUM_ROLE_API

A link to your forum role API as described in the
[Forum role API](#forum-role-api) section. Also needs to include the query parameters so if your query parameter for the
user ID is `uid`
an example URL would be `https://example.com/roleapi?uid=`. Any authorization your API requires needs to happen via
query parameters, so a resulting url would be
`https://example.com/roleapi?authkey=1234abc890&uid=`. Keep in mind that the users' ID will be appended to the end, so
the query parameter for the user ID has to be the last one! \
Role synchronisation will not work if this value is not set or an invalid URL.

#### FORUM_ROLE_API_DELAY_MS

The delay between requests to the role API in milliseconds. Has to be at least 100 and defaults to 5000 (5000
milliseconds -> 5 seconds) if no value is set. If you set a value below 100 it will still work but will use a delay of
100ms.

#### [REQUIRED] FORUM_USER_ADD_API_KEY

To add users via the API this program provides you need to set a secure API key. The API key has to be at least 64
characters long and needs to have some basic diversity, so your API key does not only consist of 64 times "A". \
Even if you do not use the API to add users, this value has to be set, or the program will not start!

#### FORUM_MEMBER_PROFILE_URL

This value defines the profile URL of your members on the forum. This only works if your forum has URLs to forum
profiles that end with the ID of the user e.g. `https://example.com/members/1234` for the member with the ID 1234. The
program only appends the ID to the URL so given the example URL you would need to set this value
to `https://example.com/members/`. If you do not set the value some commands that provide information about users might
not be able to provide a direct link to the forum user.

#### FORUM_BANNED_ROLE_ID

This value defines the role ID a user has if he is banned on the forum. Any linked user with that role will also get
banned from the Discord if the server has the permission to synchronise roles. You can only set one ID so if you have
multiple roles for different bans only one will work. If this value is not set the program obviously will not know what
the banned role is, so it will not ban anyone from the Discord based on the forum roles the user has.

#### DEFAULT_AVATAR_URL

Some commands show Discord avatars of users. However, if there is no avatar the bot can use (e.g. because the user is
not on the Discord server anymore) it will not show one. If you set this value to a URL of a picture it will use that
picture as a default avatar. If this value is not set it just won't show an avatar if the avatar is unknown. The URL has
to be a valid URL according to Discord
(include "http(s)") and has a maximum length of 2000 characters.

#### ERROR_REDIRECT_URL

If someone requests a webpage that this application does not provide he gets forwarded to an error page. This
application does not include an own error page so the user gets redirected to an error page. You can set the URL for
that page if you want to. If this value is not set it will redirect the user
to [some annoying website](https://theannoyingsite.com/) as he has no reason to be there. If you do not host other stuff
on the server the only people to ever access this page are probably bots that try to find a security vulnerability or
people that use the API to add users who should not get redirected to that page anyway.

### Database

#### Creation

On the first start the program has to create the database which needs some action on your side. To perform that setup,
add the following line to the `.env` file:

```dotenv
SPRING_PROFILES_ACTIVE=firststart
```

Afterwards start the bot as described in
[starting and stopping the bot](#starting-and-stopping-the-bot). Wait until the bot is shown as online in Discord and
stop the program as described in the same section. Now remove the line you added to the `.env` file. \
Your database is now set up, and you can start and stop the program as you like. However, do not add the line back
to `.env` as that will lead to the program creating the database again and thus deleting all your data!

#### Adding your server

If you want to let the bot create the following data automatically you need to add the bot to your server after you
started it so if you already added the bot to your server you need to kick the bot, start the bot and then add the bot
back to your server. Afterwards you can use the
[permission update command](#discord-command-to-grant-and-deny-guild-permissions)
to update your server permissions. \
However, you can also add the needed information to the database by hand. To be able to connect to the database download
[this file](https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar)
and run it with the following command:

```shell
java -cp h2-1.4.200.jar org.h2.tools.Shell
```

Now you need to provide the credentials of your database, first the URL:
`jdbc:h2:<path_to_database.mv.db>`. The database is located in `./data/`
of this program and has the name you chose in the
[DB_DATABASE](#db_database) section. If you move the file you downloaded to the same location of this README, and the
database has the default name "database" then the URL is `jdbc:h2:./data/database`. \
The next thing to enter is the driver which has to be `org.h2.Driver`. If the command line already shows that driver you
can also just press enter. \
The next input is the user you set [here](#required-db_user-and-db_password). Just type the username and press enter.
Afterwards type the password and press enter again. \
You should now see a line like this at the bottom:

```shell
sql>
```

That means you connected to the database so now information can be added. To add your guild enter the following line
with your data (read below):

```h2
INSERT INTO DISCORD_GUILD(GUILD_ID, AUTOKICK, AUTOKICK_HOUR_DELAY, READ_PERM, ROLE_SYNC_PERM, WRITE_PERM)
VALUES (id, true, 24, true, true, true);
```

The following list describes the values you can set chronologically:

1. `id`: The Discord ID of your guild. Check
   [this guide](https://support.discord.com/hc/en-us/articles/206346498-Wie-finde-ich-meine-Server-ID-)
   on how to find your Discord guild ID.
2. `true`: The first `true` enables the bot to kick users automatically if they joined over x hours ago and are not
   linked to a forum account. You can set this to `false` if you want.
3. `24`: The delay in hours for the autokick. If a user joined over x hours ago and is not linked it gets kicked. Can be
   set to any value between 12 and 168 (7 days), the default is 24.
4. `true`: The second `true` enables the permission for the guild to read specific data from the database like what
   forum user a Discord user is linked to. Keep this on `true` or you will not be able to use a lot of functionality.
   However, any new server the bot gets added to, has this disabled by default.
5. `true`: The third `true` enables role synchronisation between the forum and Discord for that guild. Keep this
   on `true` if you want to use this feature in your server.
6. `true`: The last `true` enables write permissions for the guild, so it can add users, delete users, whitelist users
   and so on. It is similar to the read permission in terms of additional info.

After adding your guild to the database type `quit` and press enter or press `ctrl+d` to leave the database console. You
can now start the bot and use its features in your server.

## Starting and stopping the bot

To start the bot you can just run the provided `start.sh` file like this:

```shell
sh start.sh
```

To stop the bot you can use:

```shell
sh stop.sh
```

For these scripts to work make sure to not delete the file `pid.txt` while the program is running. If `stop.sh` does not
work for some reason you can also do

```shell
./gradlew --stop
```

which stops all running Gradle programs with the same Gradle version, so be sure that no other Gradle programs are
running or restart them afterwards.

## Updating guild (server) data in your database

Any request to/update of the database requires you to connect to the database like in
the [Adding your server](#adding-your-server) section. The information you typed in that section should be saved, so you
just need to press enter if the data is correct. \
It does not matter if the program is running or not. Afterwards type
`quit` or press `ctrl+d` to leave the database console.

### Updating permissions of guilds (servers)

To grant a permission use the following command and set the data accordingly:

```h2
UPDATE DISCORD_GUILD
SET xyz = true
WHERE GUILD_ID = 123;
```

Replace `123` with your guild ID and replace `xyz` with one of the following permissions:

```text
AUTOKICK
READ_PERM
ROLE_SYNC_PERM
WRITE_PERM
```

You can also grant multiple permissions at once:

```h2
UPDATE DISCORD_GUILD
SET xyz1 = true,
    xyz2 = true,
    xyz3 = true
WHERE GUILD_ID = 123;
```

To deny a permission just replace `true` with `false`.

### Updating the autokick delay

To update the autokick delay use the following command:

```h2
UPDATE DISCORD_GUILD
SET AUTOKICK_HOUR_DELAY = xyz
WHERE GUILD_ID = 123;
```

Replace `123` with your guild ID and replace `xyz` with the delay in hours between 12 and 168.

### Accessing and updating any other data

If you have basic knowledge of SQL you can use the database console to access any saved data or even update some data by
hand. Please do not play around in the database if you do not know what you are doing!

### Discord command to grant and deny guild permissions

The Discord command to update permissions of a guild is `updateguildperms`
and has the following syntax `updateguildperms guildId "permission" state`. This command can only be used by the owner
of the bot (the user who
[created the bot in the developer portal](#discord-bot-token)) but can be used in any guild the bot is in as well. The
placeholders in there need to be filled with your data:

* `guildId`: The ID of the guild you want to update. Check
  [this guide](https://support.discord.com/hc/en-us/articles/206346498-Wie-finde-ich-meine-Server-ID-)
  on how to find your Discord guild ID. If you do not specify the guild ID the bot will use the ID of the guild you
  wrote the command in.
* `permission`: The name of the permission you want to update. You can control the following permissions
    * `read`: Activates the read permission of the guild, so it can request data for users.
    * `write`: Activates the write permission of the guild, so it can add, delete and change data for users.
    * `sync`: Activates role synchronisation between forum and Discord for that guild.
    * `autkick`: Activates automatic kicking of users that are not linked to a forum account.
* `state`: Can be `true` or `false`. If you do not specify the state it will choose `false` and thus disable the given
  permission for the guild.

## Credits

* [leThrax](https://github.com/leThrax) for the original idea to code this bot and first efforts to implement the
  functionality.
* [MinnDevelopment](https://github.com/MinnDevelopment),
  [DV8FromTheWorld](https://github.com/DV8FromTheWorld) and other contributors for developing and contributing to the
  [JDA library](https://github.com/DV8FromTheWorld/JDA)
  which this bot uses for the Discord side of things.

---

## Developer information

The following information is meant for people who want to add functionality to the bot or contribute in any other way.
You do not need to understand anything below to use this program.

### Profiles

This program currently offers two profiles. The default profile, and a developer profile called "dev" which has debug
outputs and other features that make developing and debugging easier. To change the profile open the `.env` file and add
the following line:

```dotenv
SPRING_PROFILES_ACTIVE=dev
```

### Adding commands and event listeners

To add a command to the bot there are a few steps to perform. First add a new entry to the `CommandInfo` class. The next
step is to create the command class in
`com.motorbesitzen.rolewatcher.bot.command.impl`. The command class needs to extend `CommandImpl`. The command needs to
be a `@Service` and needs to have its command set as a value in lowercase. So a command like `help` would be the
following:

```java

@Service("help")
public class Help extends CommandImpl {
	// ...
}
```

The same applies for event listeners although they do not need a name and thus no value:

```java

@Service
public class SomeEventListener extends ListenerAdapter {
	// ...
}
```

### Decisions

#### Why does this program use `Long` for the Discord IDs?

The Java part of this program takes Discord IDs as `Long` so the maximum ID is 9223372036854775807. If Discord does not
change its system and still uses the Twitter snowflake ID system then this ID will be reached around June 2084. If for
whatever reason Discord, the used technologies or this code should still be around at that time the code has to be
changed to accept `BigInteger` to avoid overflows while handling IDs as Discord uses the full 2<sup>64</sup>
range while the Java `Long` only uses 2<sup>63</sup>-1. 

