package com.enea.antibookban.util;

import com.enea.antibookban.config.PluginConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Messages {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private Messages() {
    }

    /** Sends a message from the config with the placeholders replaced. */
    public static void send(CommandSender sender, PluginConfig config, String key, Object... placeholders) {
        sender.sendMessage(SERIALIZER.deserialize(config.message(key, placeholders)));
    }
}
