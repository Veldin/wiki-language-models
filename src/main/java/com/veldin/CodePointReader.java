package com.veldin;

import com.veldin.consumers.CodePointConsumer;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CodePointReader {

    private static final int BUFFER_SIZE = 16 * 1024;

    public static void read(Path path, CodePointConsumer consumer) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];

        AtomicBoolean lastWasWhitespace = new AtomicBoolean(false);

        try (Reader reader = Files.newBufferedReader(path)) {

            int len;
            char pendingHigh = 0;

            // Helper: apply "collapse all whitespace runs to one ASCII space"
            // at the moment we have a full code point.
            java.util.function.IntConsumer emitCodePoint = (int cp) -> {
                if (Character.isWhitespace(cp)) {
                    if (!lastWasWhitespace.get()) {
                        consumer.accept(' ');
                        lastWasWhitespace.set(true);
                    }
                } else {
                    if (cp < 128) {
                        consumer.accept((char) cp);
                    } else {
                        consumer.accept(cp);
                    }
                    lastWasWhitespace.set(false);
                }
            };

            while ((len = reader.read(buffer)) != -1) {

                int i = 0;

                // Complete a surrogate pair that was split across buffers
                if (pendingHigh != 0) {
                    if (len > 0 && Character.isLowSurrogate(buffer[0])) {
                        int cp = Character.toCodePoint(pendingHigh, buffer[0]);
                        emitCodePoint.accept(cp);
                        i = 1;
                    } else {
                        // pendingHigh wasn't actually paired here
                        emitCodePoint.accept(pendingHigh);
                    }
                    pendingHigh = 0;
                }

                for (; i < len; i++) {

                    char c = buffer[i];

                    if (c < 128) {
                        // ASCII fast path
                        if (Character.isWhitespace(c)) {
                            if (!lastWasWhitespace.get()) {
                                consumer.accept(' ');
                                lastWasWhitespace.set(true);
                            }
                        } else {
                            consumer.accept(c);
                            lastWasWhitespace.set(false);
                        }
                        continue;
                    }

                    if (Character.isHighSurrogate(c)) {
                        if (i + 1 < len) {
                            int cp = Character.toCodePoint(c, buffer[++i]);
                            emitCodePoint.accept(cp);
                        } else {
                            pendingHigh = c;
                        }
                    } else {
                        // BMP non-surrogate: treat this char as a code point
                        emitCodePoint.accept(c);
                    }
                }
            }

            // Trailing pending high surrogate (if any)
            if (pendingHigh != 0) {
                emitCodePoint.accept(pendingHigh);
            }
        }
    }
}
