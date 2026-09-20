package me.wolfii.clienttimers.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

/**
 * Reads one command token up to whitespace, including characters such as {@code :} and {@code /}
 * that Brigadier's unquoted {@code string} argument rejects.
 */
public final class TokenArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = List.of("18:00", "2:00pm", "5min", "20t");

    private TokenArgument() {
    }

    public static TokenArgument token() {
        return new TokenArgument();
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "value");
        }
        if (StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readQuotedString();
        }
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        String value = reader.getString().substring(start, reader.getCursor());
        if (value.isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "value");
        }
        return value;
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
