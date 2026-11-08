package com.stackforge.kidtrack.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the KidTrack Platform backend.
 *
 * <p>The application is a modular monolith organised in six bounded contexts
 * (iam, subscription, stakeholder, fleet, trip and notifications) plus a shared kernel.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KidTrackPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidTrackPlatformApplication.class, args);
    }
}
