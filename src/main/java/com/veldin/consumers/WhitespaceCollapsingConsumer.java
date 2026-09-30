package com.veldin.consumers;

public final class WhitespaceCollapsingConsumer
        implements CodePointConsumer {

    private final CodePointConsumer consumer;

    private boolean lastWasWhitespace = false;

    public WhitespaceCollapsingConsumer(
            CodePointConsumer consumer
    ) {
        this.consumer = consumer;
    }

    @Override
    public void accept(int codePoint) {

        if (Character.isWhitespace(codePoint)) {

            if (!lastWasWhitespace) {
                consumer.accept(' ');
                lastWasWhitespace = true;
            }

            return;
        }

        consumer.accept(codePoint);
        lastWasWhitespace = false;
    }
}