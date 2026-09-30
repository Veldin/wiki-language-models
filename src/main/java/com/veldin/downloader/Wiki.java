package com.veldin.downloader;

import static com.veldin.downloader.WikiDumpDownloader.DUMP_BASE_URL;

public enum Wiki {

    DUTCH(
            "nlwiki",
            "nl",
            DUMP_BASE_URL + "nlwiki" + "/"
    ),

    ENGLISH(
            "enwiki",
            "en",
            DUMP_BASE_URL + "enwiki" + "/"
    ),

    GERMAN(
            "dewiki",
            "de",
            DUMP_BASE_URL + "dewiki" + "/"
    ),

    FRENCH(
            "frwiki",
            "fr",
            DUMP_BASE_URL + "frwiki" + "/"
    );

    private final String dumpName;
    private final String directoryName;
    private final String indexUrl;

    Wiki(
            String dumpName,
            String directoryName,
            String indexUrl
    ) {
        this.dumpName = dumpName;
        this.directoryName = directoryName;
        this.indexUrl = indexUrl;
    }

    public String dumpName() {
        return dumpName;
    }

    public String directoryName() {
        return directoryName;
    }

    public String indexUrl() {
        return indexUrl;
    }
}