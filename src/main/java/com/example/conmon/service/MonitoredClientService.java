package com.example.conmon.service;

import com.example.conmon.config.ConfigurationFileProperties;
import com.example.conmon.dto.ClientRequest;
import com.example.conmon.dto.ClientResponse;
import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.repository.MonitoredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MonitoredClientService {

    private final MonitoredClientRepository repository;
    private final IpAddressNormalizer normalizer;
    private final ConfigurationFileProperties configurationFileProperties;

    public MonitoredClientService(MonitoredClientRepository repository, IpAddressNormalizer normalizer,
                                  ConfigurationFileProperties configurationFileProperties) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.configurationFileProperties = configurationFileProperties;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        assertMutable();
        return toResponse(repository.save(new MonitoredClient(
                request.clientName(),
                request.description(),
                normalizer.normalize(request.ipAddress()),
                request.tags(),
                request.enabled()
        )));
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        assertMutable();
        MonitoredClient client = find(id);
        client.update(request.clientName(), request.description(), normalizer.normalize(request.ipAddress()),
                request.tags(), request.enabled());
        return toResponse(client);
    }

    @Transactional
    public void delete(UUID id) {
        assertMutable();
        repository.delete(find(id));
    }

    private void assertMutable() {
        if (configurationFileProperties.readOnly()) {
            throw new ReadOnlyConfigurationException("Configuration is read-only because fixed file mode is enabled");
        }
    }

    private MonitoredClient find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Client not found: " + id));
    }

    private ClientResponse toResponse(MonitoredClient client) {
        return new ClientResponse(client.getId(), client.getClientName(), client.getDescription(), client.getIpAddress(),
                client.getTags(), client.isEnabled(), client.getCreatedAt(), client.getUpdatedAt());
    }
}
