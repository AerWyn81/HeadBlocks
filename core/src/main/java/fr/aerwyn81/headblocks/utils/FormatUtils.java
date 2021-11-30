package fr.aerwyn81.headblocks.utils;

import com.google.common.base.Strings;
import fr.aerwyn81.headblocks.HeadBlocks;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatUtils {
	private static final Pattern hexPattern = Pattern.compile("\\{#[0-9a-fA-F]{6}}");
	private static final Pattern locPattern = Pattern.compile("^\\[([a-zA-Z]+),([\\-0-9.]+),([\\-0-9.]+),([\\-0-9.]+)]");

	/**
	 * Format a message with chat format and color (& or hexa)
	 * <p>
	 * Support MC Version 12.2 -> 1.16+
	 *
	 * @param message with {#RRGGBB}
	 * @return Formatted string to be displayed by SpigotAPI
	 */
	public static String translate(String message) {
		String replaced = message;
		Matcher m = hexPattern.matcher(replaced);
		while (m.find()) {
			String hexcode = m.group();
			String fixed = hexcode.substring(1, 8);

			try {
				Method ofMethod = ChatColor.class.getMethod("of", String.class);
				replaced = replaced.replace(hexcode, ofMethod.invoke(null, fixed).toString());
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
			}
		}
		return ChatColor.translateAlternateColorCodes('&', replaced);
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

		return translate(Strings.repeat(completedColor + symbol, progressBars) +
				Strings.repeat(notCompletedColor + symbol, totalBars - progressBars));
	}

	/**
	 * Parse int and return null if not an int
	 *
	 * @param value String
	 * @return the integer or not if not
	 */
	public static Integer parseIntOrNull(Object value) {
		try {
			return Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @param locInString Location in string
	 * @return Location bukkit or null if cannot parse
	 */
	public static Location parseLocation(String locInString) {
		if (locInString == null || locInString.trim().isEmpty()) {
			return null;
		}

		Matcher matcher = locPattern.matcher(locInString);
		if (matcher.find()) {
			try {
				return new Location(Bukkit.getWorld(matcher.group(1)), Double.parseDouble(matcher.group(2)), Double.parseDouble(matcher.group(3)), Double.parseDouble(matcher.group(4)));
			} catch (Exception ignored) {
			}
		}

		return null;
	}

	/**
	 * Handle PAPI placeholders
	 *
	 * @param player  the player to parse placeholders
	 * @param message the message with placeholders
	 * @return message with placeholders parsed or the default message
	 */
	public static String TryToFormatPlaceholders(Player player, String message) {
		message = translate(message);

		if (!HeadBlocks.isPlaceholderApiActive)
			return message;

		return PlaceholderAPI.setPlaceholders(player, message);
	}
}
