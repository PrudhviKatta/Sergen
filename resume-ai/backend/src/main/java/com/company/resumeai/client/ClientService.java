package com.company.resumeai.client;

import com.company.resumeai.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * Clients are shared across candidates (§8.2), so creating the same client name
     * twice is expected, not an error. If the normalized name already exists, that
     * existing row is returned as-is rather than creating a duplicate.
     */
    @Transactional
    public Client create(ClientCreateRequest request) {
        String normalizedName = ClientNameNormalizer.normalize(request.name());
        return clientRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> clientRepository.save(
                        new Client(request.name(), normalizedName, request.industry(), request.description())));
    }

    @Transactional(readOnly = true)
    public Client getById(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
    }
}
