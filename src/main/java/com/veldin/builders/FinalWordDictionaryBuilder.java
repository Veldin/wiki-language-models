package com.veldin.builders;

import com.veldin.finalmodels.FinalWordDictionary;
import com.veldin.tokens.TokenSequence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.veldin.finalmodels.FinalWordDictionary.packedLength;
import static com.veldin.finalmodels.FinalWordDictionary.tokenSequenceToString;

public final class FinalWordDictionaryBuilder {

    private FinalWordDictionaryBuilder() {
    }

    /**
     * Saves the dictionary to disk.
     */
    public static void save(
            Path path,
            FinalWordDictionary wordDictionary
    ) {
        Objects.requireNonNull(path, "path");

        try {
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (
                    DataOutputStream out =
                            new DataOutputStream(
                                    new BufferedOutputStream(
                                            Files.newOutputStream(path)
                                    )
                            )
            ) {
                out.writeInt(ModelFileFormatStatics.MAGIC);
                out.writeInt(ModelFileFormatStatics.TYPE_DICTIONARY);

                out.writeInt(wordDictionary.size());
                out.writeInt(wordDictionary.data.length);

                for (int length : wordDictionary.lengths) {
                    out.writeInt(length);
                }

                out.write(wordDictionary.data);
            }

            // ----- companion text file -----
            Path txtPath = path.resolveSibling(
                    path.getFileName().toString() + ".txt"
            );

            try (BufferedWriter writer =
                         Files.newBufferedWriter(
                                 txtPath,
                                 StandardCharsets.UTF_8
                         )) {

                for (int i = 0; i < wordDictionary.size(); i++) {
                    TokenSequence seq = wordDictionary.getWord(i);
                    writer.write(tokenSequenceToString(seq));
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to save word dictionary: " + path,
                    e
            );
        }
    }

    /**
     * Loads a previously saved dictionary.
     */
    public static FinalWordDictionary load(Path path) {
        Objects.requireNonNull(path, "path");

        try (
                DataInputStream in =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(path)
                                )
                        )
        ) {
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

            if (type != ModelFileFormatStatics.TYPE_DICTIONARY) {
                throw new IOException(
                        "Invalid model file type. "
                                + "Expected dictionary ("
                                + ModelFileFormatStatics.TYPE_DICTIONARY
                                + "), got "
                                + type
                );
            }

            int wordCount = in.readInt();
            int dataLength = in.readInt();

            if (wordCount < 0) {
                throw new IOException(
                        "Invalid word count: " + wordCount
                );
            }

            if (dataLength < 0) {
                throw new IOException(
                        "Invalid data length: " + dataLength
                );
            }

            int[] lengths = new int[wordCount];

            long expectedDataLength = 0;

            for (int i = 0; i < wordCount; i++) {
                int length = in.readInt();

                if (length <= 0) {
                    throw new IOException(
                            "Invalid word length at index "
                                    + i
                                    + ": "
                                    + length
                    );
                }

                lengths[i] = length;
                expectedDataLength += packedLength(length);
            }

            if (expectedDataLength != dataLength) {
                throw new IOException(
                        "Corrupt dictionary: expected "
                                + expectedDataLength
                                + " data bytes, found "
                                + dataLength
                );
            }

            byte[] data = new byte[dataLength];
            in.readFully(data);

            int[] offsets = new int[wordCount + 1];

            int offset = 0;

            for (int i = 0; i < wordCount; i++) {
                offsets[i] = offset;
                offset += packedLength(lengths[i]);
            }

            offsets[wordCount] = offset;

            FinalWordDictionary dictionary =
                    new FinalWordDictionary(
                            offsets,
                            lengths,
                            data
                    );

            dictionary.validateOrdering();

            return dictionary;

        } catch (EOFException e) {
            throw new UncheckedIOException(
                    "Truncated word dictionary: " + path,
                    e
            );
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to load word dictionary: " + path,
                    e
            );
        }
    }
}