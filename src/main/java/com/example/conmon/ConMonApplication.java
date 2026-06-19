package com.example.conmon;

import com.example.conmon.config.MonitorProperties;
import com.example.conmon.config.ConfigurationFileProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MonitorProperties.class, ConfigurationFileProperties.class})
public class ConMonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConMonApplication.class, args);
    }
}
