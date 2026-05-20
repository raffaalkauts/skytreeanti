package com.wiredid.skytree.model;

import net.kyori.adventure.text.Component;
import java.util.UUID;

public record ChatMessage(UUID senderId, String senderName, Component message, long timestamp) {
}
