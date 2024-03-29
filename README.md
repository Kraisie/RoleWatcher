# RoleWatcher

This is a Discord bot which can sync roles between a Discord and a forum. That's useful if you offer a Discord server
for the members of a forum that has changing roles due to memberships or other changing permission sets but that does
not grant database access.

## Setup

The following text is written for linux based systems. Windows or Mac systems might need slightly different setup steps
and a different startup and stop script. This bot runs on Java 11, so you need to install the Java 11 JDK for your
system.

### Prerequisites

While not specifically needed a basic understanding of computers, databases (SQL) and web traffic will help you a lot.
It might not be that easy otherwise, especially if you need to code the
[Forum role API](#forum-role-api) yourself.

#### Forum IDs

Your forum needs to have user and role IDs that are numeric and start by 1. Your highest possible ID should be 2<sup>
63</sup>-1. If your forum uses so-called "Snowflakes" as IDs there might be issues as Discord uses that ID system too.
While duplicates between forum IDs and Discord IDs are very unlikely they can happen if your forum uses them as well.

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

#### Forum selfadd API/webhook

While you can link users with the provided commands you can also build a site on your forum to let users link their
accounts themselves. You will need to send the following JSON object to your server:

```json
{
  "uid": 1234,
  "username": "abc",
  "verificationcode": "def"
}
```

Obviously you need to replace the data in that object with the user data. However, the maximum length of
the `verificationCode` is 20 characters! \
Send that payload to `/users` and add a query parameter `key` which gets described in
[FORUM_USER_ADD_API_KEY](#required-forum_user_add_api_key). An example URL would look like

this: `https://example.com/users?key=4875944894BNADCBelsafs5487346dn`. \
The reply may contain an error message in plain text, but the status codes are more important:

* `204`: Everything went okay, and the bot received the code for the user
* `400`: The verification code is already reserved for another user
* `409`: The user is already linked
* `422`: The bot received invalid data
* `5XX`: Some server-sided issue, check the logs for further information

After that the user has to verify with the provided verification command and the verification code in your Discord
server. The used Discord account and the sent forum user information get linked to each other if the code matches.

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
Kick Members
Ban Members
View Channels
Send Messages
Embed Links
Mention everyone
Use external emojis
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
* The bot will only react to commands in channels it can read/write messages in.
* Not granting the bot the needed permissions might lead to unknown behaviour.
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

BOT_ACTIVITY=
BOT_ACTIVITY_TEXT=
BOT_ACTIVITY_STREAMING_URL=

FORUM_ROLE_API_URL=
FORUM_ROLE_API_DELAY_MS=
FORUM_USER_ADD_API_KEY=
FORUM_MEMBER_PROFILE_URL=
FORUM_BANNED_ROLE_ID=
```

In the following sections each of these settings will be explained. For each of them the following points need to be
met:

* Do not write text in multiple lines after the equal sign (`=`).
* Make sure your lines do not have trailing spaces or tabs!
* Encapsulate text with spaces in it with quotation marks (`"`).

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
an example URL would be `https://example.com/roleapi?uid`. Any authorization your API requires needs to happen via query
parameters, so a resulting url would be
`https://example.com/roleapi?authkey=1234abc890&uid`. Keep in mind that the users' ID will be appended to the end, so
the query parameter for the user ID has to be the last one! The program add the needed `=` itself! \
Role synchronisation will not work if this value is not set or if the URL is invalid.

#### FORUM_ROLE_API_DELAY_MS

The delay between requests to the role API in milliseconds. Has to be at least 100 and defaults to 5000 (5000
milliseconds -> 5 seconds) if no value is set. If you set a value below 100 it will still work but it will use a delay
of 100ms.

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
work for some reason you can also search for the `java` process and kill it manually.

## Updating guilds (servers)

### Discord command to grant and deny guild permissions

While you can manually change the permissions in the database you would need to stop the bot before connecting to the
database. To avoid that downtime you can use the `updateperms` command which does not get shown in the help command as
only the bot owner can use it. \
The Discord command to update permissions of a guild is `updateperms`
and has the following syntax `updateperms guildId "permission" state`. This command can only be used by the owner of the
bot (the user who
[created the bot in the developer portal](#discord-bot-token)) but the owner can use it in any guild the bot is in. The
placeholders in there need to be filled with your data:

* `guildId`: The ID of the guild you want to update. Check
  [this guide](https://support.discord.com/hc/en-us/articles/206346498-Wie-finde-ich-meine-Server-ID-)
  on how to find your Discord guild ID. If you do not specify the guild ID the bot will use the ID of the guild you
  wrote the command in.
* `permission`: The name of the permission you want to update. You can control the following permissions
  * `read`: Activates the read permission of the guild, so it can request data for users.
  * `write`: Activates the write permission of the guild, so it can add, delete and change data for users.
  * `sync`: Activates role synchronisation between forum and Discord for that guild.
  * `autokick`: Activates automatic kicking of users that are not linked to a forum account after a set delay.
* `state`: Can be `true` or `false`. If you do not specify the state it will choose `false` and thus disable the given
  permission for the guild.

### Accessing and updating any other data

If you have basic knowledge of SQL you can use the H2 shell to access any saved data or even update some data by hand.
Due to the integration into the bot you can only access the database while the bot is not running! Only way to access
the database while the bot is running is to use the developer profile; **THAT PROFILE WILL WIPE YOUR DATABASE ON EACH
RESTART SO DO NOT USE IT OUTSIDE OF DEVELOPMENT!!!** \
Please do not play around in the database if you do not know what you are doing! Almost everything can be controlled
with Discord commands or by using the features described in this README. There is no need to touch the database at all.

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

This program currently offers a few profiles. The default profile (production), and the developer profile called "dev"
are probably the most important ones. The debug profile has debug outputs and other features that make developing and
debugging easier. To change the profile to the developer profile open the `.env` file and add the following line:

```dotenv
SPRING_PROFILES_ACTIVE=dev
```

The same effect can be achieved by changing the run configuration of your IDE to use that profile.

The database creation process of hibernate will try to import a file called `import.sql` in the `resources`
folder on startup which can be used for sample data. However, each SQL statement needs to be in one line!

### Adding commands

To add a command to the bot there are a few steps to perform. First create the command class in
`com.motorbesitzen.rolewatcher.bot.command.impl`. The command class needs to extend `CommandImpl`. The command needs to
be a `@Service` and needs to have its command set as a value in lowercase. So a command like `help` would be the
following:

```java

@Service("help")
public class Help extends CommandImpl {
  // ...
}
```

Your IDE or the compiler should notify you about the methods you need to implement for a command to function.

### Adding event listeners

Event listeners do not need a name and thus no special value. Just annotate the listener class as a service and make
sure it extends the `ListenerAdapter`. Override at least one of the `ListenerAdapter` methods, so your event listener
actually does something.

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
changed to accept `BigInteger`. To avoid overflows while handling IDs as Discord uses the full 2<sup>64</sup>
range while the Java `Long` only go up to 2<sup>63</sup>-1. 
