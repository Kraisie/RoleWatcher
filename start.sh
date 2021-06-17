#!/bin/sh
echo Starting the bot...

# Build the project with gradle
echo "$(printf '\t')"Building bot...
./gradlew bootJar >/dev/null 2>&1

# create the environment variables for the bot
echo "$(printf '\t')"Creating environment...
set -a
. ./.env
set +a

# execute the program with java to avoid gradle overhead
echo "$(printf '\t')"Executing bot...
java -jar ./build/libs/rolewatcher.jar </dev/null >/dev/null 2>&1 &
echo $! > pid.txt

echo Started the bot. The bot will appear online in a few seconds.