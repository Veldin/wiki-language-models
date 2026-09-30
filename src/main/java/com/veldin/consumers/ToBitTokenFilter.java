package com.veldin.consumers;

import com.veldin.tokens.TokenCodec;

public final class ToBitTokenFilter implements CodePointConsumer {

    private final CodePointConsumer consumer;
    private final TokenCodec token;

    public ToBitTokenFilter(CodePointConsumer consumer, TokenCodec token) {
        this.consumer = consumer;
        this.token = token;
    }

    @Override
    public void accept(int codePoint) {
        consumer.accept(
                token.toToken(codePoint)
        );
    }

    public void finish() {
        consumer.finish();
    }

}