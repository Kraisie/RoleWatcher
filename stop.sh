echo Stopping the bot...
value=$(cat pid.txt)
kill "$value"
echo Stopped the bot.