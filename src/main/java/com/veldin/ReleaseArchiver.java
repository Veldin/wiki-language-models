package com.veldin;

import com.veldin.builders.ModelFileFormatStatics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.veldin.LanguageModelsCreators.MODELS_ROOT;
import static com.veldin.Main.OUTPUT_ROOT;

public final class ReleaseArchiver {

    private static final Path RELEASE_ROOT =
            OUTPUT_ROOT.resolve("releases");

    private ReleaseArchiver() {
    }

    public static Path archive(
            Release release
    ) throws IOException {

        String releaseName =
                release.getName();

        Path releaseDirectory =
                RELEASE_ROOT.resolve(releaseName);

        Files.createDirectories(releaseDirectory);

        Path archive =
                releaseDirectory.resolve(
                        releaseName + ".zip"
                );

        Path sourceDirectory =
                MODELS_ROOT
                        .resolve(release.dumpRecord.wiki().directoryName())
                        .resolve(release.dumpRecord.dumpDate());

        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException(
                    "Output directory does not exist: "
                            + sourceDirectory
            );
        }

        if (Files.isRegularFile(archive)) {
            throw new IOException(
                    "Output archive already exist: "
                            + archive
            );
        }

        // Write dump and model format metadata.
        var wiki = release.dumpRecord.wiki();

        String dumpInfoContent = """
                wiki: %s
                dumpName: %s
                directoryName: %s
                indexUrl: %s
                dumpDate: %s
                filename: %s
                dumpUrl: %s
                fileUrl: %s
                DictionaryVersion: %d
                VocabularyVersion: %d
                NgramVersion: %d
                """.formatted(
                yamlValue(wiki.name()),
                yamlValue(wiki.dumpName()),
                yamlValue(wiki.directoryName()),
                yamlValue(wiki.indexUrl()),
                yamlValue(release.dumpRecord.dumpDate()),
                yamlValue(release.dumpRecord.filename()),
                yamlValue(release.dumpRecord.dumpUrl()),
                yamlValue(release.dumpRecord.fileUrl()),
                ModelFileFormatStatics.TYPE_DICTIONARY,
                ModelFileFormatStatics.TYPE_VOCABULARY,
                ModelFileFormatStatics.TYPE_NGRAM
        );

        Files.writeString(
                releaseDirectory.resolve("dump-info.yaml"),
                dumpInfoContent
        );

        List<Path> files;

        try (var stream = Files.walk(sourceDirectory)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .toList();
        }

        // Copy TXT files directly into the release directory.
        for (Path file : files) {
            if (file.getFileName()
                    .toString()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".txt")) {

                Path target =
                        releaseDirectory.resolve(
                                file.getFileName().toString()
                        );

                Files.copy(file, target);
            }
        }

        // Only non-TXT files go into the ZIP.
        List<Path> filesToArchive =
                files.stream()
                        .filter(file ->
                                !file.getFileName()
                                        .toString()
                                        .toLowerCase(Locale.ROOT)
                                        .endsWith(".txt"))
                        .toList();

        long totalBytes = 0;

        for (Path file : filesToArchive) {
            totalBytes += Files.size(file);
        }

        Progress progress =
                new Progress(
                        "Creating " + releaseName,
                        totalBytes,
                        "bytes"
                );

        try (
                OutputStream output =
                        Files.newOutputStream(archive);

                ZipOutputStream zip =
                        new ZipOutputStream(output)
        ) {

            byte[] buffer = new byte[8192];

            for (Path file : filesToArchive) {

                Path relative =
                        sourceDirectory.relativize(file);

                ZipEntry entry =
                        new ZipEntry(
                                relative
                                        .toString()
                                        .replace('\\', '/')
                        );

                zip.putNextEntry(entry);

                try (InputStream input =
                             Files.newInputStream(file)) {

                    int read;

                    while ((read = input.read(buffer)) != -1) {
                        zip.write(buffer, 0, read);
                        progress.next(read);
                    }
                }

                zip.closeEntry();
            }
        }

        return releaseDirectory;
    }

    private static String yamlValue(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }
}