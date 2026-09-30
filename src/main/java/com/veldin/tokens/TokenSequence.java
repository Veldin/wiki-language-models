package com.veldin.tokens;

import java.util.Arrays;

public final class TokenSequence {

    private final byte[] tokens;
    private final int hash;

    public TokenSequence(
            byte[] source,
            int length
    ) {

        this.tokens =
                Arrays.copyOf(
                        source,
                        length
                );

        this.hash =
                Arrays.hashCode(tokens);
    }

    public int size() {
        return tokens.length;
    }

    public int get(int index) {

        return tokens[index] & SevenBitToken.TOKEN_MASK;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TokenSequence other)) {
            return false;
        }

        return Arrays.equals(
                tokens,
                other.tokens
        );
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return toString(SixBitWordToken.instance);
    }

    public String toString(TokenCodec codec) {

        StringBuilder result =
                new StringBuilder(tokens.length);

        for (byte token : tokens) {

            int value =
                    token & codec.getTokenMask();

            result.append(
                    Character.toChars(
                            codec.toCodePoint(value)
                    )
            );
        }

        return result.toString();
    }
}