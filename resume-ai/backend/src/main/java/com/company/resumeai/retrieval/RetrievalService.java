package com.company.resumeai.retrieval;

import com.company.resumeai.embedding.EmbeddingClient;
import com.company.resumeai.embedding.VectorCodec;
import com.company.resumeai.knowledge.KnowledgeFragment;
import com.company.resumeai.knowledge.KnowledgeFragmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * §13 hybrid retrieval: structured filters (handled in the repository's WHERE
 * clause) plus pgvector cosine-distance ranking, in one query. Diversity
 * selection (§13 step 3 - don't return ten near-duplicate fragments from the
 * same source) is NOT implemented here; it belongs with the generation
 * pipeline (Milestone 5) that will actually consume these results, not this
 * infrastructure layer.
 */
@Service
public class RetrievalService {

    private static final int DEFAULT_LIMIT = 10;

    private final EmbeddingClient embeddingClient;
    private final KnowledgeFragmentRepository knowledgeFragmentRepository;

    public RetrievalService(EmbeddingClient embeddingClient, KnowledgeFragmentRepository knowledgeFragmentRepository) {
        this.embeddingClient = embeddingClient;
        this.knowledgeFragmentRepository = knowledgeFragmentRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeFragment> retrieveSimilar(String queryText, RetrievalFilter filter, int limit) {
        float[] queryVector = embeddingClient.embed(queryText);
        String queryVectorLiteral = VectorCodec.encode(queryVector);
        return knowledgeFragmentRepository.findSimilar(
                queryVectorLiteral,
                filter.domain(),
                filter.role(),
                filter.clientId(),
                filter.startYear(),
                filter.endYear(),
                limit > 0 ? limit : DEFAULT_LIMIT
        );
    }
}
