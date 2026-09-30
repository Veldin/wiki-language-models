package com.veldin;

import com.veldin.downloader.Wiki;
import com.veldin.downloader.WikiDumpDownloader;
import com.veldin.downloader.WikiDumpRecord;
import com.veldin.extractor.WikiDumpExtractor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static Path DOWNLOAD_WIKIPEDIA_ROOT = Path.of("target", "wikipedia");

    public static Path OUTPUT_ROOT = Path.of("target", "generated-models");

    public static void main(String[] args) {

        // First argument overwrites the root path
        if (args.length > 0) {
            DOWNLOAD_WIKIPEDIA_ROOT = Path.of(args[0]);
        }

        // Second argument overwrites the output path
        if (args.length > 1) {
            OUTPUT_ROOT = Path.of(args[1]);
        }

        System.out.println("Max heap: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");

        List<Wiki> languages = List.of(Wiki.FRENCH, Wiki.GERMAN, Wiki.ENGLISH, Wiki.DUTCH);
        // List<Wiki> languages = List.of(Wiki.POLISH);

        // Prepare download operations
        List<Throwing<WikiDumpRecord>> dumpDownloadOperations = new ArrayList<>();

        for (Wiki language : languages) {
            dumpDownloadOperations.add(() -> WikiDumpDownloader.downloadLatest(language));
        }

        // Execute downloads
        List<ResultEx<WikiDumpRecord>> dumpDownloadResults = ResultTry.doTryMultipleAsync(dumpDownloadOperations);

        for (int i = 0; i < dumpDownloadResults.size(); i++) {
            ResultEx<WikiDumpRecord> result = dumpDownloadResults.get(i);

            if (result.isOk()) {
                System.out.println("Downloaded: " + result.unwrap().wiki().dumpName());
            } else {
                System.err.println("Failed to download " + languages.get(i) + ": " + result.unwrapError());
            }
        }

        // Prepare extraction operations
        List<Throwing<WikiDumpRecord>> extractOperations = new ArrayList<>();

        for (ResultEx<WikiDumpRecord> record : dumpDownloadResults) {
            if (record.isOk()) {
                extractOperations.add(() -> WikiDumpExtractor.extract(record.unwrap()));
            }
        }

        // Execute extraction
        List<ResultEx<WikiDumpRecord>> extractionResults = ResultTry.doTryMultipleAsync(extractOperations);

        for (ResultEx<WikiDumpRecord> result : extractionResults) {
            if (result.isOk()) {
                System.out.println("Extracted: " + result.unwrap().wiki().dumpName());
            } else {
                System.err.println("Failed to extract: " + result.unwrapError());
            }
        }

        // Prepare language-model creation operations
        List<Throwing<Release>> createModelOperations = new ArrayList<>();

        for (ResultEx<WikiDumpRecord> record : extractionResults) {
            if (record.isOk()) {
                createModelOperations.add(() -> {
                    LanguageModelsCreators model = new LanguageModelsCreators(record.unwrap());

                    return model.generate();
                });
            }
        }

        // Execute language-model creation
        // Explicitly not Async, as the memory cost of the model creation is huge.
        List<ResultEx<Release>> finalNGramGeneratorResults = ResultTry.doTryMultiple(createModelOperations);

        // Collect successfully created models
        List<Release> releases = new ArrayList<>(finalNGramGeneratorResults.size());

        for (ResultEx<Release> result : finalNGramGeneratorResults) {
            if (result.isOk()) {
                releases.add(result.unwrap());

                System.out.println("Created model: " + result.unwrap().getName());
            } else {
                System.err.println("Failed to create language model: " + result.unwrapError());
            }
        }

        // Prepare release archiving operations
        List<Throwing<Path>> createReleaseOperations = new ArrayList<>();

        for (Release release : releases) {
            createReleaseOperations.add(() -> ReleaseArchiver.archive(release));
        }

        // Execute release archiving
        List<ResultEx<Path>> releaseResults = ResultTry.doTryMultipleAsync(createReleaseOperations);

        // Log release results
        for (ResultEx<Path> result : releaseResults) {
            if (result.isOk()) {
                System.out.println("Created release archive: " + result.unwrap());
            } else {
                System.err.println("Failed to create release: " + result.unwrapError());
            }
        }
    }
}