package com.wiredid.skytree.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for converting legacy color codes to Adventure Components.
 * Simplifies migration from deprecated String-based ItemMeta methods.
 */
public class ComponentUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Parse a string using MiniMessage if it contains tags, else legacy codes.
     */
    public static Component smartParse(String text) {
        if (text == null)
            return Component.empty();

        // Convert & to § for legacy support
        String translated = text.replace('&', '§');

        // Handle custom <n\d> tags before MiniMessage (protect them or convert them)
        // If they are resource pack icons, we might want to keep them but MiniMessage
        // will fail if they aren't registered.
        // We'll wrap them in <literal> or just escape them if needed.

        if (translated.contains("<") && translated.contains(">")) {
            // Convert main legacy codes to MiniMessage tags
            String mm = translated
                    .replace("§0", "<black>").replace("§1", "<dark_blue>").replace("§2", "<dark_green>")
                    .replace("§3", "<dark_aqua>").replace("§4", "<dark_red>").replace("§5", "<dark_purple>")
                    .replace("§6", "<gold>").replace("§7", "<gray>").replace("§8", "<dark_gray>")
                    .replace("§9", "<blue>").replace("§a", "<green>").replace("§b", "<aqua>")
                    .replace("§c", "<red>").replace("§d", "<light_purple>").replace("§e", "<yellow>")
                    .replace("§f", "<white>").replace("§l", "<b>").replace("§m", "<st>")
                    .replace("§n", "<u>").replace("§o", "<i>").replace("§r", "<reset>");

            try {
                return MINI_MESSAGE.deserialize(mm);
            } catch (Exception e) {
                // If MiniMessage fails (due to unknown tags like <n3>), fall back to legacy
                // parse but strip brackets or escape them
                return parse(translated);
            }
        }
        return parse(translated);
    }

    /**
     * Parse a legacy string (with § color codes) into a Component
     */
    public static Component parse(String text) {
        if (text == null)
            return Component.empty();
        return SERIALIZER.deserialize(text.replace('&', '§'));
    }

    /**
     * Parse multiple legacy strings into a List of Components
     */
    public static List<Component> parseList(String... lines) {
        return Arrays.stream(lines)
                .map(ComponentUtil::parse)
                .collect(Collectors.toList());
    }

    /**
     * Parse a List of legacy strings into Components
     */
    public static List<Component> parseList(List<String> lines) {
        if (lines == null)
            return List.of();
        return lines.stream()
                .map(ComponentUtil::parse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Component back to legacy string (for comparisons)
     */
    public static String toLegacy(Component component) {
        if (component == null)
            return "";
        return SERIALIZER.serialize(component);
    }

    /**
     * Strip all color and formatting from a Component
     */
    public static String stripColor(Component component) {
        if (component == null)
            return "";
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

}
