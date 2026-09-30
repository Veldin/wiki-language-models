package com.veldin.builders;

public final class ModelFileFormatStatics {

    public static final int MAGIC = 0x564C4D31; // VLM1

    public static final int TYPE_DICTIONARY = 1;
    public static final int TYPE_VOCABULARY = 2;
    public static final int TYPE_NGRAM = 3;

    private ModelFileFormatStatics() {
    }
}