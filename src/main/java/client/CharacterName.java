package client;

import java.nio.charset.StandardCharsets;

/** Name limits shared with OpenStory: 13-byte zero-terminated v83 field. */
public final class CharacterName {
    private CharacterName() {}

    public static boolean isValid(String name) {
        return name != null && name.matches("[a-zA-Z0-9一-鿿]{3,12}")
                && name.getBytes(StandardCharsets.UTF_8).length <= 12;
    }
}
