package com.example.conmon.controller;

import com.example.conmon.dto.ConfigurationDocument;
import com.example.conmon.service.ConfigurationDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuration")
@Tag(name = "Configuration")
public class ConfigurationController {

    private final ConfigurationDocumentService configurationDocumentService;

    public ConfigurationController(ConfigurationDocumentService configurationDocumentService) {
        this.configurationDocumentService = configurationDocumentService;
    }

    @GetMapping
    public ConfigurationDocument getConfiguration() {
        return configurationDocumentService.exportConfiguration();
    }

    @PostMapping
    public ConfigurationDocument importConfiguration(@Valid @RequestBody ConfigurationDocument document,
                                                      @RequestParam(defaultValue = "false") boolean replaceExisting) {
        configurationDocumentService.importConfiguration(document, replaceExisting);
        return configurationDocumentService.exportConfiguration();
    }
}
