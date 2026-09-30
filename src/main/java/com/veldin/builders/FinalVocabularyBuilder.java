package com.veldin.builders;

import com.veldin.finalmodels.FinalVocabulary;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FinalVocabularyBuilder {

    private FinalVocabularyBuilder() {
    }

    /*
     * ============================================================
     * SAVE
     * ============================================================
     */

    public static void save(
            Path file,
            FinalVocabulary vocabulary
    ) throws IOException {

        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (DataOutputStream out =
                     new DataOutputStream(
                             Files.newOutputStream(file))) {

            // Common model file header
            out.writeInt(
                    ModelFileFormatStatics.MAGIC
            );

            out.writeInt(
                    ModelFileFormatStatics.TYPE_VOCABULARY
            );

            // Vocabulary data
            for (long sequence :
                    vocabulary.getSequences()) {

                out.writeLong(sequence);
            }
        }

        Path textFile =
                Path.of(
                        file + ".txt"
                );

        Files.writeString(
                textFile,
                vocabulary.toString()
        );
    }

    /*
     * ============================================================
     * LOAD
     * ============================================================
     */

    public static FinalVocabulary load(
            Path file
    ) throws IOException {

        long[] sequences =
                new long[
                        FinalVocabulary.VOCABULARY_SIZE
                        ];

        try (DataInputStream in =
                     new DataInputStream(
                             Files.newInputStream(file))) {

            // ----- common model file header -----

            int magic = in.readInt();

            if (magic != ModelFileFormatStatics.MAGIC) {
                throw new IOException(
                        "Invalid model file. "
                                + "Expected magic 0x"
                                + Integer.toHexString(
                                ModelFileFormatStatics.MAGIC
                        )
                                + ", got 0x"
                                + Integer.toHexString(magic)
                );
            }

            int type = in.readInt();

            if (type != ModelFileFormatStatics.TYPE_VOCABULARY) {
                throw new IOException(
                        "Invalid model file type. "
                                + "Expected vocabulary ("
                                + ModelFileFormatStatics.TYPE_VOCABULARY
                                + "), got "
                                + type
                );
            }

            // ----- vocabulary data -----

            for (int i = 0;
                 i < sequences.length;
                 i++) {

                sequences[i] =
                        in.readLong();
            }
        } catch (EOFException e) {
            throw new IOException(
                    "Truncated vocabulary model: " + file,
                    e
            );
        }

        return new FinalVocabulary(
                sequences
        );
    }
}