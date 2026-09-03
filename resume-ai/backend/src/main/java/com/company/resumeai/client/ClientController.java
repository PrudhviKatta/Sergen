package com.company.resumeai.client;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientCreateRequest request) {
        Client client = clientService.create(request);
        ClientResponse body = ClientResponse.from(client);
        return ResponseEntity.created(URI.create("/api/v1/clients/" + client.getId())).body(body);
    }
}
