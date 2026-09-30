package com.veldin;

import com.veldin.downloader.WikiDumpRecord;

import java.util.Locale;

public class Release {

    public final WikiDumpRecord dumpRecord;
    public final FinalLanguageModels models;

    public Release(WikiDumpRecord dumpRecord, FinalLanguageModels models) {
        this.dumpRecord = dumpRecord;
        this.models = models;
    }

    public String getName() {
        return String.format(
                Locale.ROOT,
                "veldin-language-model-%s-%s",
                dumpRecord.wiki().directoryName(),
                dumpRecord.dumpDate()
        );
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                dumpRecord,
                models
        );
    }
}



