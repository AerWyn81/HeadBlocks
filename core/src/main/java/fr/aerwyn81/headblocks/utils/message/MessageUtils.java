package fr.aerwyn81.headblocks.utils.message;

import com.google.common.base.Strings;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
	private static final Pattern hexPattern = Pattern.compile("\\{#[0-9a-fA-F]{6}}");

	/**
	 * Format a message with chat format and color (& or hexa)
	 * Support MC Version 12.2 -> 1.16+
	 *
	 * @param message with {#RRGGBB}
	 * @return Formatted string to be displayed by SpigotAPI
	 */
	public static String colorize(String message) {
		String replaced = message;
		Matcher m = hexPattern.matcher(replaced);
		while (m.find()) {
			String hexcode = m.group();
			String fixed = hexcode.substring(1, 8);

			try {
				Method ofMethod = ChatColor.class.getMethod("of", String.class);
				replaced = replaced.replace(hexcode, ofMethod.invoke(null, fixed).toString());
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) { }
		}
		return ChatColor.translateAlternateColorCodes('&', replaced);
	}

	/**
	 * Replace placeholders x y z world by location in string
	 * @param message string
	 * @param location location for replace
	 * @return parsed string
	 */
	public static String parseLocationPlaceholders(String message, Location location) {
		return message.replaceAll("%x%", String.valueOf(location.getBlockX()))
				.replaceAll("%y%", String.valueOf(location.getBlockY()))
				.replaceAll("%z%", String.valueOf(location.getBlockZ()))
				.replaceAll("%world%", parseWorld(location))
				.replaceAll("%worldName%", parseWorld(location));
	}

	private static String parseWorld(Location location) {
		return location.getWorld() != null ? location.getWorld().getName() : colorize("&cUnknownWorld");
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
		int progressBars = (int) (totalBars * percent);

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
