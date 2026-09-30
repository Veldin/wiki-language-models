package com.veldin;

import com.veldin.consumers.CodePointConsumer;
import com.veldin.consumers.ToBitTokenFilter;
import com.veldin.consumers.WhitespaceCollapsingConsumer;
import com.veldin.consumers.WikiExtractorFilter;
import com.veldin.tokens.TokenCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class FileSpider {

    private final Path root;
    private final TokenCodec tokenCodec;
    private final CodePointConsumer consumer;

    public FileSpider(
            Path root,
            TokenCodec tokenCodec,
            CodePointConsumer consumer
    ) {
        this.root = root;
        this.tokenCodec = tokenCodec;
        this.consumer = consumer;
    }

    public void run() throws IOException {

        /*
         * Collect the files first so we know the total count.
         */
        List<Path> files;

        try (var stream = Files.walk(root)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .toList();
        }

        int totalFiles = files.size();
        int processedFiles = 0;

        long startTime = System.nanoTime();

        String lastProgressLine = "";

        ToBitTokenFilter toBitTokenFilter =
                new ToBitTokenFilter(
                        consumer,
                        tokenCodec
                );

        WhitespaceCollapsingConsumer whitespace =
                new WhitespaceCollapsingConsumer(
                        toBitTokenFilter
                );

        WikiExtractorFilter wiki =
                new WikiExtractorFilter(
                        whitespace
                );

        for (Path file : files) {
            try {
                CodePointReader.read(
                        file,
                        wiki
                );

                processedFiles++;

                long elapsedNanos =
                        System.nanoTime() - startTime;

                long remainingNanos =
                        estimateRemainingTime(
                                elapsedNanos,
                                processedFiles,
                                totalFiles
                        );

                String progressLine = String.format(
                        "Progress: %d/%d files | Remaining: %d | Elapsed: %s | Estimated remaining: %s",
                        processedFiles,
                        totalFiles,
                        totalFiles - processedFiles,
                        formatDuration(elapsedNanos),
                        formatDuration(remainingNanos)
                );

                /*
                 * Move back to the beginning of the current line and
                 * overwrite the previous progress message.
                 */
                int padding =
                        Math.max(
                                0,
                                lastProgressLine.length()
                                        - progressLine.length()
                        );

                System.out.print(
                        "\r"
                                + progressLine
                                + " ".repeat(padding)
                );

                System.out.flush();

                lastProgressLine = progressLine;

            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to process file: " + file,
                        e
                );
            }
        }

        /*
         * Move to a new line before printing the final message.
         */
        System.out.println();

        long totalElapsedNanos =
                System.nanoTime() - startTime;

        System.out.printf(
                "Finished processing %d files in %s%n",
                processedFiles,
                formatDuration(totalElapsedNanos)
        );

        consumer.finish();
    }

    private static long estimateRemainingTime(
            long elapsedNanos,
            int processedFiles,
            int totalFiles
    ) {
        int remainingFiles =
                totalFiles - processedFiles;

        if (processedFiles == 0 || remainingFiles <= 0) {
            return 0L;
        }

        long averageTimePerFile =
                elapsedNanos / processedFiles;

        return averageTimePerFile * remainingFiles;
    }

    private static String formatDuration(long nanos) {
        long totalSeconds =
                TimeUnit.NANOSECONDS.toSeconds(nanos);

        long hours =
                totalSeconds / 3600;

        long minutes =
                (totalSeconds % 3600) / 60;

        long seconds =
                totalSeconds % 60;

        if (hours > 0) {
            return String.format(
                    "%dh %02dm %02ds",
                    hours,
                    minutes,
                    seconds
            );
        }

        if (minutes > 0) {
            return String.format(
                    "%dm %02ds",
                    minutes,
                    seconds
            );
        }

        return String.format(
                "%ds",
                seconds
        );
    }
}
