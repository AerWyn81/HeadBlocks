package fr.aerwyn81.headblocks.utils.message;

import com.google.common.base.Strings;
import fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class MessageUtils {

	/**
	 * Format a message with chat format and color (& or hexa)
	 * Support MC Version 12.2 -> 1.16+
	 *
	 * @param message with {#RRGGBB}
	 * @return Formatted string to be displayed by SpigotAPI
	 */
	public static String colorize(String message) {
		return IridiumColorAPI.process(message);
	}

	public static String unColorize(String name) {
		return IridiumColorAPI.stripColorFormatting(name);
	}

	/**
	 * Clean message string
	 * @param message to translate
	 * @return clean message translated
	 */
	public static String centerMessage(String message) {
		return message.contains("{center}") ? sendCenteredString(message.replaceAll("\\{center}", "")) : message;
	}

	/**
	 * Create a progress bar
	 *
	 * @param current           current progression
	 * @param max               max progression
	 * @param totalBars         number of bars
	 * @param symbol            symbol to show
	 * @param completedColor    color when completed
	 * @param notCompletedColor color when not completed
	 * @return progress bar string
	 */
	public static String createProgressBar(int current, int max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
		float percent = (float) current / max;
		int progressBars = Math.min((int) (totalBars * percent), 100);

		return colorize(Strings.repeat(completedColor + symbol, progressBars) +
				Strings.repeat(notCompletedColor + symbol, totalBars - progressBars));
	}

	/**
	 * Return a random list of random colors
	 * @return list of color
	 */
	public static ArrayList<Color> getRandomColors() {
		ArrayList<Color> colors = new ArrayList<>();
		for (int i = 0; i < ThreadLocalRandom.current().nextInt(1, 4); i++) {
			colors.add(Color.fromRGB(ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(256)));
		}

		return colors;
	}

	/**
	 * Allow to center a message in chat
	 * Thanks to @SirSpoodles
	 * @param message to center
	 * @return string centered
	 */
	public static String sendCenteredString(String message) {
		String[] lines = message.split("\n", 40);
		StringBuilder returnMessage = new StringBuilder();

		for (String line : lines) {
			int toCompensate = 154 - calculatePxSize(line) / 2;
			int spaceLength = DefaultFont.SPACE.getLength() + 1;
			int compensated = 0;
			StringBuilder sb = new StringBuilder();

			if (toCompensate < 0) {
				String[] words = line.split(" ");

				StringBuilder subLine = new StringBuilder();
				StringBuilder subLineBelow = new StringBuilder();
				for (String word : words) {
					if (calculatePxSize(word + subLine) <= 154) {
						if (subLine.length() != 0) {
							subLine.append(" ");
						}

						subLine.append(word);
					} else {
						if (subLineBelow.length() != 0) {
							subLineBelow.append(" ");
						}

						subLineBelow.append(word);
					}
				}

				returnMessage.append(sendCenteredString(subLine.toString())).append("\n").append(sendCenteredString(subLineBelow.toString()));
				continue;
			} else {
				while (compensated < toCompensate) {
					sb.append(" ");
					compensated += spaceLength;
				}
			}

			returnMessage.append(sb).append(line).append("\n");
		}

    	return returnMessage.toString();
	}

	private static int calculatePxSize(String message) {
		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;
		for (char c : message.toCharArray()) {
			if (c == '§') {
				previousCode = true;
			} else if (previousCode) {
				previousCode = false;
				isBold = (c == 'l');
			} else {
				DefaultFont dFI = DefaultFont.getDefaultFontInfo(c);
				messagePxSize = isBold ? (messagePxSize + dFI.getBoldLength()) : (messagePxSize + dFI.getLength());
				messagePxSize++;
			}
		}

		return messagePxSize;
	}
}
