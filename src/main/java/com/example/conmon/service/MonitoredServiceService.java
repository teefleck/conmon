package com.example.conmon.service;

import com.example.conmon.config.ConfigurationFileProperties;
import com.example.conmon.dto.ServiceRequest;
import com.example.conmon.dto.ServiceResponse;
import com.example.conmon.entity.MonitoredService;
import com.example.conmon.repository.MonitoredServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MonitoredServiceService {

    private final MonitoredServiceRepository repository;
    private final ConfigurationFileProperties configurationFileProperties;

    public MonitoredServiceService(MonitoredServiceRepository repository, ConfigurationFileProperties configurationFileProperties) {
        this.repository = repository;
        this.configurationFileProperties = configurationFileProperties;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        assertMutable();
        return toResponse(repository.save(new MonitoredService(
                request.serviceName(),
                request.description(),
                request.port(),
                request.tags(),
                request.enabled()
        )));
    }

    @Transactional
    public ServiceResponse update(UUID id, ServiceRequest request) {
        assertMutable();
        MonitoredService service = find(id);
        service.update(request.serviceName(), request.description(), request.port(), request.tags(), request.enabled());
        return toResponse(service);
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

    private MonitoredService find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }

    private ServiceResponse toResponse(MonitoredService service) {
        return new ServiceResponse(service.getId(), service.getServiceName(), service.getDescription(), service.getPort(),
                service.getTags(), service.isEnabled(), service.getCreatedAt(), service.getUpdatedAt());
    }
}
