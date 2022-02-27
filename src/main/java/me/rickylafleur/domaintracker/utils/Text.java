package me.rickylafleur.domaintracker.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ricky Lafleur
 */
public final class Text {

    private final static Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String colorize(String message) {
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            final String hexCode = message.substring(matcher.start(), matcher.end());
            final String replaceSharp = hexCode.replace('#', 'x');

            final char[] ch = replaceSharp.toCharArray();
            final StringBuilder builder = new StringBuilder();

            for (final char c : ch) {
                builder.append("&").append(c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}