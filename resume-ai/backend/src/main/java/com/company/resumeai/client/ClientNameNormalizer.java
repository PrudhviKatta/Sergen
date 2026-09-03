package com.company.resumeai.client;

/**
 * Minimal placeholder for §29 "Client Validation" (e.g. "J.P. Morgan" / "JPMC" ->
 * "JPMorgan Chase"). That needs an alias/lookup table which is out of scope for
 * Milestone 1. For now this only trims/collapses whitespace and upper-cases, so
 * near-duplicate free-text names at least collide on the uq_client_normalized_name
 * constraint. Replace with real alias resolution before Milestone 4/5.
 */
final class ClientNameNormalizer {

    private ClientNameNormalizer() {
    }

    static String normalize(String rawName) {
        return rawName.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
