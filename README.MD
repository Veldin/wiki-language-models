# Welcome to my statistical language model generation tool

This repo contains a program that can automatically download latest wikipedia dumps, extracts it using
WikiExtractor/wikiextractor and creates (statistical, non-ai) language models.

This was made to facilitate the creation for basic old-fashioned spelling correction and prediction tools. This because
when you now try to use the google search bar to fix typo's they just rspond with some wierd AI instead of telling you
where your typos are.

# Pipeline

The application follows some rough stages that are;

- Download the wikis of the configured languages.
- Extract the wikis into usable text.
- Use the text to create the language models.
    - Create a dictionary (Unique words)
    - Learn a vocabulary
    - Train a ngram language model (using the vocab)

# 6-Bit Word Token

Instead of storing every character as a full Java char, we give each supported character a small 6-bit number (0-63).
These numbers can then be packed together into a long, so an entire word can fit into a single value.

This makes the vocabulary and n-gram data much smaller and faster to work with. It also gives us a simple way to handle
things like spaces, sentence endings, and unknown characters. Basically, we turn words into tiny numbers because doing
that millions of times adds up.

# generated-modules

## Dictionary

The dictionary stores words as the **6-Bit Word Tokens** instead of characters. Words are sorted, deduplicated, and then
packed
into a single byte array.

**4 tokens = 24 bits = 3 bytes**, so the packed representation uses **0.75 bytes per token**.

```text
tokens:  012345 012345 012345 012345
bytes:   01234501 23450123 45012345
```

The binary file contains:

```text
MAGIC | VERSION | WORD COUNT | DATA LENGTH | LENGTHS[] | PACKED DATA[]
```

Because the words are sorted, exact lookups use binary search. For spelling correction, the dictionary first narrows the
search to words sharing the first two tokens and then compares them using Levenshtein distance.

A `.txt` companion file is also generated for word inspection.

## Vocabulary

The vocabulary contains 255 entries, each represented by a single 64-bit long.

Each long stores a sequence of 6-bit character tokens. With 6 bits per character, one long can contain up to 10
characters:

10 × 6 = 60 bits

The binary file simply contains the 255 long values consecutively. Each sequence's position in the file is its
vocabulary token. Since every entry is exactly 8 bytes, the sequence for token n can be read directly at:

To decode a sequence, read the 6-bit values from the long starting at the lowest bits, shifting right by 6 bits after
each character. A zero token marks the end of the sequence. The decoded characters are then reversed to restore their
original order.

The accompanying '.txt' file contains the same vocabulary in human-readable form and is intended for
inspection/debugging
rather than model use.

## N-Gram Model

the N-Gram Model stores a mapping from a packed context to up to two predictions.

Each entry in the model is exactly 6 bytes.
4 bytes context, and two 1 byte predictions.

The file contains entries directly after each other. There is a 8 bit header. 
4 bytes 'VLM1' magic and 4 bytes indicating the model type.

The file size minus the header must be divisible by 6, so the number of entries can be calculated directly ((file size - 8) / 6).

The model uses the vocabulary tokens as its input and packs the previous tokens into a single Integer.

Depending on how much history is available, the context represents a different N-gram.

The context packs four 8 bit of history into one 32-bit integer. For longer context, tokens are mixed using rotate-XOR
before being packed. This keeps the final context as a fixed 4 bytes, regardless of the amount of history.

The 2-byte prediction field contains up to two 1-byte vocabulary tokens. The first prediction is the most likely next
token. The second prediction is an optional second most likely token. (The value 255 means that no second prediction
exist)

To use the model, the previous vocabulary tokens should be passed using the same packing strategy that was used when the
model was created. The resulting integer is the key used in model's vocab lookup table.

# Licensing

The language models in this project are generated from Wikipedia dump data provided by the Wikimedia Foundation.

Wikipedia text is generally available under the Creative Commons Attribution-ShareAlike 4.0 International License (CC
BY-SA 4.0) and the GNU Free Documentation License (GFDL).

## Generated language models

The generated language models are derived from Wikipedia text and therefore may be subject to the licensing requirements
applicable to the source material. In particular, redistribution of material derived from Wikipedia text may require
attribution and compliance with the applicable ShareAlike terms.

These models are statistical language models generated from the source text; the original Wikipedia articles are not
included as readable text in the model files.

When redistributing one of these models, please retain this licensing information and identify the Wikipedia project and
dump date from which the model was generated.