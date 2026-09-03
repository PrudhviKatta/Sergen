package com.company.resumeai.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class VectorCodecTest {

    @Test
    void encodesAsPgvectorTextLiteral() {
        String encoded = VectorCodec.encode(new float[]{0.1f, 0.2f, -0.3f});

        assertThat(encoded).isEqualTo("[0.1,0.2,-0.3]");
    }

    @Test
    void roundTripsThroughEncodeAndDecode() {
        float[] original = {0.123f, -4.5f, 0.0f, 999.999f};

        float[] decoded = VectorCodec.decode(VectorCodec.encode(original));

        assertThat(decoded).hasSize(original.length);
        for (int i = 0; i < original.length; i++) {
            assertThat(decoded[i]).isCloseTo(original[i], offset(0.0001f));
        }
    }

    @Test
    void decodesEmptyVector() {
        float[] decoded = VectorCodec.decode("[]");

        assertThat(decoded).isEmpty();
    }
}
