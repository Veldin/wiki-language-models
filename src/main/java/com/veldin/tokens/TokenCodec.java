package com.veldin.tokens;

public interface TokenCodec {

    int toToken(int codePoint);

    int toCodePoint(int token);

    int getTokenMask();
}