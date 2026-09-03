package com.company.resumeai.embedding;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Converts between a Java {@code float[]} and pgvector's text literal format
 * (e.g. {@code [0.12,0.34,-0.01]}) - see knowledge.KnowledgeFragment, which
 * stores the column as a plain String with a Hibernate @ColumnTransformer
 * casting it to/from the real `vector` column type. Kept dependency-free
 * (no pgvector-java / hypersistence-utils) so Milestone 3 doesn't take on an
 * unverified third-party Hibernate integration - this is ~10 lines either way.
 */
public final class VectorCodec {

    private VectorCodec() {
    }

    public static String encode(float[] vector) {
        String joined = IntStream.range(0, vector.length)
                .mapToObj(i -> Float.toString(vector[i]))
                .collect(Collectors.joining(","));
        return "[" + joined + "]";
    }

    public static float[] decode(String literal) {
        String trimmed = literal.trim();
        String inner = trimmed.substring(1, trimmed.length() - 1); // strip [ ]
        if (inner.isEmpty()) {
            return new float[0];
        }
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
