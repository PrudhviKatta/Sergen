package com.company.resumeai.project;

import com.company.resumeai.candidate.Candidate;
import com.company.resumeai.candidate.CandidateService;
import com.company.resumeai.client.Client;
import com.company.resumeai.client.ClientService;
import com.company.resumeai.validation.ChronologyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CandidateProjectService {

    private final CandidateProjectRepository candidateProjectRepository;
    private final CandidateService candidateService;
    private final ClientService clientService;

    public CandidateProjectService(CandidateProjectRepository candidateProjectRepository,
                                    CandidateService candidateService,
                                    ClientService clientService) {
        this.candidateProjectRepository = candidateProjectRepository;
        this.candidateService = candidateService;
        this.clientService = clientService;
    }

    @Transactional
    public CandidateProject create(UUID candidateId, ProjectCreateRequest request) {
        // §29 Chronology Validation: reject before it ever hits the DB check constraint.
        ChronologyValidator.requireStartNotAfterEnd(request.startDate(), request.endDate());

        Candidate candidate = candidateService.getById(candidateId);
        Client client = clientService.getById(request.clientId());

        CandidateProject project = new CandidateProject(
                candidate,
                client,
                request.projectName(),
                request.roleTitle(),
                request.startDate(),
                request.endDate(),
                request.domain(),
                request.projectSummary()
        );
        return candidateProjectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<CandidateProject> listByCandidate(UUID candidateId) {
        candidateService.getById(candidateId); // 404 if candidate doesn't exist
        return candidateProjectRepository.findByCandidateIdOrderByStartDateAsc(candidateId);
    }
}
