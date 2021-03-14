package com.motorbesitzen.rolewatcher.bot.event;

import com.motorbesitzen.rolewatcher.util.LogUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RiddleDmListener extends ListenerAdapter {

	@Override
	public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
		if (event.getAuthor().isBot()) {
			return;
		}

		final Message message = event.getMessage();
		final String rawMessage = message.getContentRaw();
		final User author = message.getAuthor();

		LogUtil.logInfo("[RIDDLE] \"" + author.getAsTag() + "\" (" + author.getId() + ") answered with: \"" + rawMessage + "\"");

		if (rawMessage.equalsIgnoreCase("Hello")) {
			message.reply(
					"Hello there, I don't always annoy people with riddles but when I do you can expect the biggest cringe of them all. " +
							"Lets start with a 4/10 one:\n" +
							"\"Okay, now listen: The riddle is easy. First, read everything and answer here to solve. Is there anything unclear?\n" +
							"The first solver can name an EO subscription of his choice at the end (one month). Discord staff are furries!\""
			).queue();
			return;
		}

		if (rawMessage.equalsIgnoreCase("M0t0rB3s1tzen")) {
			message.reply(
					"Great, you got the hang of things! There is a little story for these riddles in which *you* play " +
							"the main role.\nLike a lot of people all over the world you woke up this morning and thought \"" +
							"Man, I would die for some good **bacon** right now!\". However, you do not have any available in " +
							"your AirBnB right now so you choose to order from the restaurant down the street. As you take a " +
							"look on your phone to order it just shows some numbers and does not react to any input. The phone shows:\n" +
							"0011100000101010010000011 0000110110 000000111001101010100101001101"
			).queue();
			return;
		}

		if (rawMessage.equalsIgnoreCase("HAXED BY APOLLO")) {
			message.reply(
					"As your phone is still not functioning you go to the restaurant on foot. On the " +
							"restaurants' menu you find the following entry:\n*9. Caesars salad - $$$*\n" +
							"The rest is not in a language you understand so you just order the salad. " +
							"As you are not sure how much you need to pay you check the bill:\n" +
							"**erdv kyv ljvierdv fw kyv ljvi nzky kyv dfjk czbvj fe vexzevfnezex**\n" +
							"As there is no price you just leave the restaurant."
			).queue();
			return;
		}

		if (rawMessage.equalsIgnoreCase("crotle")) {
			message.reply(
					"The owner of the restaurant catches you while leaving and wants you to pay. You do not have " +
							"the needed currency and the owner does not want your weird paper payment. The owner signalizes " +
							"that you can pay off the salad by working. He leads you to something that looks like a computer.\n" +
							"It shows a program in a language you can understand and that you know a little about. You " +
							"see an error on the monitor that you recognize and know how to fix: Synchronize the windows time.\n" +
							"What is the EngineOwning error code (0x...) you see on that computer?")
					.queue();
			return;
		}

		if (rawMessage.toLowerCase().contains("8001011f")) {
			message.reply(
					"The restaurant owner is happy that you solved his problem and lets you leave. " +
							"While going through the door he gives you some words on your way that he knows in your language:\n" +
							"\"Don't do math kids, but be equal!\"\n" +
							"It seems to be some joke that got lost in translation. The owner sees your facial expression and says " +
							"it again but this time in his language:\n" +
							"V2/hhdC+BpcyB0aG/UgeWVh+ciBFbmdp/bmV+Pd25pbm+cgZ290/IGVzdGF+/ibGlza/GVkIGl+uPw==").queue();
			return;
		}

		if (rawMessage.equals("2014")) {
			message.reply(
					"Now, since story time is over you'll need to take this one without any story aspects:\n" +
							"D5753575D47525B5561674F585879676F6C4D584B4E44454A4B5\nNote that if you solve this one you can feel as clever as Leonardo da Vinci."
			).queue();
			return;
		}

		if (rawMessage.equals("[JEDNKH]LogixX_Gae[RWMWSW]")) {
			final File riddleState = new File("riddle.txt");
			if (riddleState.exists()) {
				message.reply("Correct! You solved the riddle. Unfortunately you are not the first to solve it.").queue();
				LogUtil.logInfo("[RIDDLE] \"" + author.getAsTag() + "\" (" + author.getId() + ") solved the riddle");
			} else {
				message.reply("Correct! You solved the riddle as the first person. Congratulations!").queue();
				saveWinner(riddleState, author);
				informOwner(event.getJDA(), author);
			}

			return;
		}

		message.getChannel().sendMessage("Sorry, that is not the correct answer.").queue();
	}

	private void saveWinner(final File riddleState, final User author) {
		final String winner = author.getAsTag() + " - " + author.getId();
		try {
			Files.writeString(Path.of(riddleState.getPath()), winner);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void informOwner(final JDA jda, final User author) {
		jda.retrieveApplicationInfo().queue(
				appInfo -> appInfo.getOwner().openPrivateChannel().queue(
						channel -> {
							channel.sendMessage(author.getAsMention() + " (" + author.getId() + ") solved the riddle!").queue();
							channel.close().queue();
						}
				)
		);
	}
}
