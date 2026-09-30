package com.veldin.downloader;

import java.nio.file.Path;

public record WikiDumpRecord(
        Wiki wiki,
        String dumpDate,
        String filename,
        String dumpUrl,
        String fileUrl,
        Path outputDirectory,
        Path outputFile
) {
}