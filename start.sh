echo Starting the bot...
# start program detached with input and output to /dev/null
nohup ./gradlew bootRun </dev/null >/dev/null 2>&1 &
echo $! > pid.txt
echo Started the bot. The bot will appear online in a few seconds.