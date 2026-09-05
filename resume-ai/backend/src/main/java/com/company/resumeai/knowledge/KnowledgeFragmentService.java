package com.company.resumeai.knowledge;

import com.company.resumeai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KnowledgeFragmentService {

    private final KnowledgeFragmentRepository knowledgeFragmentRepository;
    private final EmbeddingClient embeddingClient;

    public KnowledgeFragmentService(KnowledgeFragmentRepository knowledgeFragmentRepository,
                                     EmbeddingClient embeddingClient) {
        this.knowledgeFragmentRepository = knowledgeFragmentRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public KnowledgeFragment create(KnowledgeFragmentCreateRequest request) {
        KnowledgeFragment fragment = new KnowledgeFragment(
                request.candidateId(),
                request.clientId(),
                request.projectId(),
                request.fragmentType(),
                request.content(),
                request.domain(),
                request.role(),
                request.startYear(),
                request.endYear()
        );
        if (request.sourceResumeId() != null) {
            fragment.applySourceResume(request.sourceResumeId());
        }
        fragment.applyEmbedding(embeddingClient.embed(request.content()));
        return knowledgeFragmentRepository.save(fragment);
    }

    /** Used when deleting a resume_source - see ingestion.ResumeUploadService.delete(). */
    @Transactional
    public long deleteBySourceResumeId(UUID sourceResumeId) {
        return knowledgeFragmentRepository.deleteBySourceResumeId(sourceResumeId);
    }
}
