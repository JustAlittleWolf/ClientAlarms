package me.wolfii.clientalarms;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientAlarms {
    public static final String MOD_ID = "clientalarms";
    public static final Logger LOGGER = LoggerFactory.getLogger("ClientAlarms");

    private ClientAlarms() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
