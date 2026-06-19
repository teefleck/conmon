package com.example.conmon.service;

import com.example.conmon.config.ConfigurationFileProperties;
import com.example.conmon.config.MonitorProperties;
import com.example.conmon.dto.ConfigurationClientEntry;
import com.example.conmon.dto.ConfigurationDocument;
import com.example.conmon.dto.ConfigurationServiceEntry;
import com.example.conmon.dto.MonitorConfigurationDocument;
import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.entity.MonitoredService;
import com.example.conmon.repository.MonitoredClientRepository;
import com.example.conmon.repository.MonitoredServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class ConfigurationDocumentService {

    private final MonitoredServiceRepository serviceRepository;
    private final MonitoredClientRepository clientRepository;
    private final MonitorProperties monitorProperties;
    private final ConfigurationFileProperties configurationFileProperties;
    private final IpAddressNormalizer ipAddressNormalizer;
    private final Clock clock;

    public ConfigurationDocumentService(MonitoredServiceRepository serviceRepository,
                                        MonitoredClientRepository clientRepository,
                                        MonitorProperties monitorProperties,
                                        ConfigurationFileProperties configurationFileProperties,
                                        IpAddressNormalizer ipAddressNormalizer,
                                        Clock clock) {
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
        this.monitorProperties = monitorProperties;
        this.configurationFileProperties = configurationFileProperties;
        this.ipAddressNormalizer = ipAddressNormalizer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ConfigurationDocument exportConfiguration() {
        List<ConfigurationServiceEntry> services = serviceRepository.findAll().stream()
                .sorted(Comparator.comparing(MonitoredService::getServiceName))
                .map(this::toEntry)
                .toList();
        List<ConfigurationClientEntry> clients = clientRepository.findAll().stream()
                .sorted(Comparator.comparing(MonitoredClient::getClientName))
                .map(this::toEntry)
                .toList();
        return new ConfigurationDocument(Instant.now(clock), configurationFileProperties.readOnly(),
                blankToNull(configurationFileProperties.file()), monitor(), services, clients);
    }

    @Transactional
    public void importConfiguration(ConfigurationDocument document, boolean replaceExisting) {
        if (replaceExisting) {
            clientRepository.deleteAll();
            serviceRepository.deleteAll();
            clientRepository.flush();
            serviceRepository.flush();
        }
        for (ConfigurationServiceEntry service : nullToEmpty(document.services())) {
            importService(service);
        }
        for (ConfigurationClientEntry client : nullToEmpty(document.clients())) {
            importClient(client);
        }
    }

    private void importService(ConfigurationServiceEntry service) {
        serviceRepository.findByServiceName(service.serviceName())
                .ifPresentOrElse(existing -> existing.update(service.serviceName(), service.description(), service.port(),
                                service.tags(), service.enabled()),
                        () -> serviceRepository.save(new MonitoredService(service.id(), service.serviceName(),
                                service.description(), service.port(), service.tags(), service.enabled())));
    }

    private void importClient(ConfigurationClientEntry client) {
        String normalizedIp = ipAddressNormalizer.normalize(client.ipAddress());
        clientRepository.findByClientName(client.clientName())
                .ifPresentOrElse(existing -> existing.update(client.clientName(), client.description(), normalizedIp,
                                client.tags(), client.enabled()),
                        () -> clientRepository.save(new MonitoredClient(client.id(), client.clientName(),
                                client.description(), normalizedIp, client.tags(), client.enabled())));
    }

    private ConfigurationServiceEntry toEntry(MonitoredService service) {
        return new ConfigurationServiceEntry(service.getId(), service.getServiceName(), service.getDescription(),
                service.getPort(), service.getTags(), service.isEnabled());
    }

    private ConfigurationClientEntry toEntry(MonitoredClient client) {
        return new ConfigurationClientEntry(client.getId(), client.getClientName(), client.getDescription(),
                client.getIpAddress(), client.getTags(), client.isEnabled());
    }

    private MonitorConfigurationDocument monitor() {
        return new MonitorConfigurationDocument(monitorProperties.enabled(), monitorProperties.scanIntervalMillis(),
                monitorProperties.eventRetention(), monitorProperties.eventPersistenceEnabled(),
                monitorProperties.procNetTcpPath(), monitorProperties.procNetTcp6Path());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
