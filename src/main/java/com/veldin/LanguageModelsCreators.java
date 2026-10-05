package com.veldin;

import com.veldin.builders.FinalNGramBuilder;
import com.veldin.builders.FinalVocabularyBuilder;
import com.veldin.builders.FinalWordDictionaryBuilder;
import com.veldin.consumers.RollingTokenBuffer;
import com.veldin.downloader.WikiDumpRecord;
import com.veldin.finalmodels.FinalLanguageModels;
import com.veldin.finalmodels.FinalNGramModel;
import com.veldin.finalmodels.FinalVocabulary;
import com.veldin.finalmodels.FinalWordDictionary;
import com.veldin.learners.NGramLearner;
import com.veldin.learners.VocabularyLearner;
import com.veldin.learners.WordDictionaryLearner;
import com.veldin.ngrampackstrategy.NGramPackStrategy;
import com.veldin.ngrampackstrategy.XorPackStrategy;
import com.veldin.tokens.SixBitWordToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.veldin.Main.DOWNLOAD_WIKIPEDIA_ROOT;
import static com.veldin.Main.OUTPUT_ROOT;

public class LanguageModelsCreators {

    private static final Path EXTRACTED_ROOT =
            DOWNLOAD_WIKIPEDIA_ROOT.resolve("extracted");

    public static final Path MODELS_ROOT =
            OUTPUT_ROOT.resolve("models");

    private final WikiDumpRecord dump;

    public LanguageModelsCreators(WikiDumpRecord dump) {
        this.dump = dump;
    }

    public Release generate() throws IOException {

        /*
         * ========================================================
         * TRAINING DATA
         * ========================================================
         */

        Path trainingRoot =
                EXTRACTED_ROOT
                        .resolve(dump.wiki().directoryName())
                        .resolve(dump.dumpDate());

        if (!Files.isDirectory(trainingRoot)) {
            throw new IOException(
                    "Training directory does not exist: "
                            + trainingRoot
            );
        }

        Path modelRoot =
                MODELS_ROOT
                        .resolve(dump.wiki().directoryName())
                        .resolve(dump.dumpDate());

        Files.createDirectories(modelRoot);

        System.out.println("Training data:");
        System.out.println("  Wiki       : " + dump.wiki().dumpName());
        System.out.println("  Dump date  : " + dump.dumpDate());
        System.out.println("  Input      : " + trainingRoot);
        System.out.println();
        System.out.println("Generated model:");
        System.out.println("  Output     : " + modelRoot);
        System.out.println();

        /*
         * ========================================================
         * DICTIONARY
         * ========================================================
         */

        Path dictionaryFile =
                modelRoot.resolve("dictionary");

        FinalWordDictionary dictionary;

        if (Files.exists(dictionaryFile)) {

            System.out.println(
                    "Loading dictionary: "
                            + dictionaryFile
            );

            dictionary =
                    FinalWordDictionaryBuilder.load(
                            dictionaryFile
                    );

        } else {

            System.out.println(
                    "Building dictionary..."
            );

            WordDictionaryLearner wordDictionaryLearner =
                    new WordDictionaryLearner();

            FileSpider dictionarySpider =
                    new FileSpider(
                            trainingRoot,
                            SixBitWordToken.instance,
                            wordDictionaryLearner
                    );

            dictionarySpider.run();

            dictionary =
                    wordDictionaryLearner
                            .toFinalWordDictionary();

            FinalWordDictionaryBuilder.save(
                    dictionaryFile,
                    dictionary
            );

            System.out.println(
                    "Dictionary saved: "
                            + dictionary.size()
                            + " words"
            );
        }

        /*
         * ========================================================
         * VOCABULARY
         * ========================================================
         */

        Path vocabularyFile =
                modelRoot.resolve("vocab");

        FinalVocabulary vocabulary;

        if (Files.exists(vocabularyFile)) {

            System.out.println(
                    "Loading vocabulary: "
                            + vocabularyFile
            );

            vocabulary =
                    FinalVocabularyBuilder.load(
                            vocabularyFile
                    );

        } else {

            System.out.println(
                    "Building vocabulary..."
            );

            VocabularyLearner vocabularyLearner =
                    new VocabularyLearner();

            FileSpider vocabularySpider =
                    new FileSpider(
                            trainingRoot,
                            SixBitWordToken.instance,
                            vocabularyLearner
                    );

            vocabularySpider.run();

            long[] vocab =
                    vocabularyLearner.buildVocabulary();

            vocabulary =
                    new FinalVocabulary(vocab);

            FinalVocabularyBuilder.save(
                    vocabularyFile,
                    vocabulary
            );

            System.out.println(
                    "Vocabulary saved: "
                            + vocabularyFile
            );
        }

        /*
         * ========================================================
         * N-GRAM MODEL
         * ========================================================
         */

        Path nGramFile =
                modelRoot.resolve("ngram");

        NGramPackStrategy nGramPackStrategy =
                new XorPackStrategy();

        FinalNGramModel nGramLearner;

        if (Files.exists(nGramFile)) {

            System.out.println(
                    "Loading n-gram model: "
                            + nGramFile
            );

            nGramLearner =
                    FinalNGramBuilder.load(
                            nGramFile,
                            nGramPackStrategy
                    );

        } else {

            System.out.println(
                    "Building n-gram model..."
            );

            NGramLearner learner =
                    new NGramLearner(
                            nGramPackStrategy
                    );

            RollingTokenBuffer rollingTokenBuffer =
                    new RollingTokenBuffer(
                            vocabulary,
                            learner
                    );

            FileSpider nGramSpider =
                    new FileSpider(
                            trainingRoot,
                            SixBitWordToken.instance,
                            rollingTokenBuffer
                    );

            nGramSpider.run();

            rollingTokenBuffer.flush();

            nGramLearner =
                    learner.toFinalNGramLearner(
                            nGramPackStrategy
                    );

            FinalNGramBuilder.save(
                    nGramFile,
                    nGramLearner
            );

            System.out.println(
                    "N-gram model saved: "
                            + nGramFile
            );
        }

        /*
         * ========================================================
         * GENERATOR
         * ========================================================
         */

        FinalLanguageModels generator =
                new FinalLanguageModels(
                        dictionary,
                        vocabulary,
                        nGramLearner
                );

        Release release =
                new Release(
                        dump,
                        generator
                );

        return release;
/*
        Scanner scanner =
                new Scanner(System.in);

        System.out.println(
                "NGram generator ready. Type a prompt and press Enter."
        );
        System.out.println(
                "Type 'exit' to quit."
        );
        System.out.println();

        while (true) {

            System.out.print("> ");

            String prompt =
                    scanner.nextLine();

            if (prompt.equalsIgnoreCase("exit")) {
                break;
            }

            printUsingGenerator(
                    generator,
                    dictionary,
                    prompt
            );
        }

        scanner.close();

        return generator;

 */
    }

    private static void printUsingGenerator(
            FinalLanguageModels generator,
            FinalWordDictionary dictionary,
            String prompt
    ) {
        StringBuilder wordsInfo =
                new StringBuilder(
                        "Words within distance 3 of original prompt: "
                );

        List<String> list =
                dictionary.getAllWithinDistance(
                        prompt,
                        3
                );

        for (String l : list) {
            wordsInfo.append(l).append(' ');
        }

        System.out.println(wordsInfo);
        System.out.println();

        String correctedPrompt =
                correctSentence(
                        dictionary,
                        prompt
                );

        System.out.println(
                "Original : " + prompt
        );

        System.out.println(
                "Corrected: " + correctedPrompt
        );

        System.out.println();

        StringBuilder text =
                new StringBuilder(correctedPrompt);

        generator.generate(
                text,
                75
        );

        System.out.println(text);
        System.out.println();
    }

    private static String correctSentence(
            FinalWordDictionary dictionary,
            String sentence
    ) {
        if (sentence == null || sentence.isBlank()) {
            return sentence;
        }

        String[] words =
                sentence.trim().split("\\s+");

        List<String> corrected =
                new ArrayList<>(words.length);

        for (String word : words) {

            String closest =
                    dictionary.getClosest(word);

            corrected.add(
                    closest != null
                            ? closest
                            : word
            );
        }

        return String.join(
                " ",
                corrected
        );
    }
}