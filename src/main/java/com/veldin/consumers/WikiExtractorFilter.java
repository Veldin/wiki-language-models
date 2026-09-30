package com.veldin.consumers;

public final class WikiExtractorFilter implements CodePointConsumer {

    private final CodePointConsumer consumer;

    private boolean insideDocument = false;
    private boolean insideTag = false;

    private final StringBuilder tag =
            new StringBuilder();

    public WikiExtractorFilter(CodePointConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(int codePoint) {

        /*
         * --------------------------------------------------------
         * Currently inside a <...> tag.
         * --------------------------------------------------------
         */
        if (insideTag) {

            if (codePoint == '>') {

                insideTag = false;

                processTag();

                tag.setLength(0);

                return;
            }

            tag.appendCodePoint(codePoint);

            return;
        }

        /*
         * --------------------------------------------------------
         * Start of a tag.
         * --------------------------------------------------------
         */
        if (codePoint == '<') {

            insideTag = true;

            tag.setLength(0);
            tag.append('<');

            return;
        }

        /*
         * --------------------------------------------------------
         * Article text.
         * --------------------------------------------------------
         */
        if (insideDocument) {
            consumer.accept(codePoint);
        }
    }

    private void processTag() {

        String value =
                tag.toString()
                        .trim()
                        .toLowerCase();

        /*
         * <doc ...>
         *
         * The opening tag can contain attributes:
         *
         * <doc id="1" url="..." title="...">
         */
        if (value.startsWith("<doc ")) {

            insideDocument = true;

            /*
             * Separate the document metadata from
             * the article text.
             */
            consumer.accept(' ');

            return;
        }

        /*
         * </doc>
         */
        if (value.equals("</doc>")) {

            /*
             * Separate this article from the next one.
             */
            consumer.accept(' ');

            insideDocument = false;
        }
    }

    public void finish() {

        insideDocument = false;
        insideTag = false;

        tag.setLength(0);
    }
}