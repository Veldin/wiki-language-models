package com.veldin.consumers;

public interface CodePointConsumer {
    void accept(int codePoint);

    default void finish() {
        // called when end-of-stream is reached
    }
}