package com.veldin.extractor;

import com.veldin.downloader.Wiki;
import com.veldin.downloader.WikiDumpRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static com.veldin.Main.DOWNLOAD_WIKIPEDIA_ROOT;

public final class WikiDumpExtractor {

    private static final Path EXTRACTED_ROOT =
            DOWNLOAD_WIKIPEDIA_ROOT.resolve("extracted");

    private WikiDumpExtractor() {
    }

    public static WikiDumpRecord extract(WikiDumpRecord dump)
            throws IOException, InterruptedException {

        Wiki wiki = dump.wiki();

        Path wikiDirectory =
                EXTRACTED_ROOT
                        .resolve(wiki.directoryName());

        Path outputDirectory =
                wikiDirectory
                        .resolve(dump.dumpDate());

        /*
         * ========================================================
         * ALREADY EXTRACTED
         * ========================================================
         */

        if (Files.isDirectory(outputDirectory)) {

            System.out.println();
            System.out.println("Already extracted:");
            System.out.println(outputDirectory);

            return dump;
        }

        /*
         * ========================================================
         * REMOVE OLDER EXTRACTIONS
         * ========================================================
         */

        deleteOtherExtractions(
                wikiDirectory,
                dump.dumpDate()
        );

        /*
         * ========================================================
         * CREATE OUTPUT DIRECTORY
         * ========================================================
         */

        Files.createDirectories(outputDirectory);

        /*
         * ========================================================
         * RUN WIKIEXTRACTOR
         * ========================================================
         */

        System.out.println();
        System.out.println("============================================================");
        System.out.println("Extracting: " + wiki.dumpName());
        System.out.println("Dump date:  " + dump.dumpDate());
        System.out.println("Input:      " + dump.outputFile());
        System.out.println("Output:     " + outputDirectory);
        System.out.println("============================================================");

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "py",
                        "-m",
                        "wikiextractor.WikiExtractor",
                        dump.outputFile().toString(),
                        "-o",
                        outputDirectory.toString(),
                        "--no-templates"
                );

        processBuilder.redirectErrorStream(true);

        Process process =
                processBuilder.start();

        /*
         * ========================================================
         * PRINT PYTHON OUTPUT
         * ========================================================
         */

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream()
                                )
                        )
        ) {

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        /*
         * ========================================================
         * CHECK RESULT
         * ========================================================
         */

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IOException(
                    "WikiExtractor failed for "
                            + wiki.dumpName()
                            + " with exit code "
                            + exitCode
            );
        }

        System.out.println();
        System.out.println("Extraction complete:");
        System.out.println(outputDirectory);

        return dump;
    }

    /*
     * ============================================================
     * DELETE OTHER EXTRACTIONS
     * ============================================================
     */

    private static void deleteOtherExtractions(
            Path wikiDirectory,
            String currentDumpDate
    ) throws IOException {

        if (!Files.isDirectory(wikiDirectory)) {
            return;
        }

        try (var paths = Files.list(wikiDirectory)) {

            paths
                    .filter(Files::isDirectory)
                    .filter(path ->
                            !path.getFileName()
                                    .toString()
                                    .equals(currentDumpDate)
                    )
                    .forEach(path -> {

                        try {

                            System.out.println();
                            System.out.println(
                                    "Removing old extraction:"
                            );
                            System.out.println(path);

                            deleteRecursively(path);

                        } catch (IOException e) {

                            throw new RuntimeException(
                                    "Could not delete old extraction: "
                                            + path,
                                    e
                            );
                        }
                    });
        }
    }

    /*
     * ============================================================
     * DELETE DIRECTORY RECURSIVELY
     * ============================================================
     */

    private static void deleteRecursively(
            Path directory
    ) throws IOException {

        try (var paths = Files.walk(directory)) {

            paths
                    .sorted(
                            Comparator.reverseOrder()
                    )
                    .forEach(path -> {

                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(
                                    "Could not delete: " + path,
                                    e
                            );
                        }
                    });
        }
    }
}