package com.stackforge.kidtrack.platform.trip.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentSeverity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripType;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads a scheduled pick-up trip for today, a completed return trip and one resolved incident. */
@Component
@Order(5)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TripSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripSeeder.class);

    private final TripRepository tripRepository;
    private final IncidentRepository incidentRepository;

    public TripSeeder(TripRepository tripRepository, IncidentRepository incidentRepository) {
        this.tripRepository = tripRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tripRepository.count() > 0) {
            return;
        }
        var today = LocalDate.now().toString();
        var students = List.of(SeedIds.CHILD_LUCIA.toString(), SeedIds.CHILD_DIEGO.toString(),
                SeedIds.CHILD_MATEO.toString());

        var norte = new Trip(SeedIds.TRIP_NORTE, SeedIds.ORGANIZATION, SeedIds.ROUTE_NORTE,
                "Ruta Norte — Comas / Los Olivos", SeedIds.USER_DRIVER_CARLOS, "Carlos Ramirez", SeedIds.VEHICLE_ABC,
                "ABC-123", students, TripType.OUTBOUND, today, "06:00");
        tripRepository.save(norte);

        var este = new Trip(SeedIds.TRIP_ESTE, SeedIds.ORGANIZATION, SeedIds.ROUTE_ESTE,
                "Ruta Este — San Juan de Lurigancho", SeedIds.USER_DRIVER_LUIS, "Luis Torres", SeedIds.VEHICLE_DEF,
                "DEF-456", students.subList(0, 2), TripType.RETURN, today, "14:30");
        var finished = Instant.now().minus(2, ChronoUnit.HOURS);
        este.start(finished.minus(45, ChronoUnit.MINUTES), "I.E. San Marcos (salida)");
        este.updateProgress(este.getStudentsTotal(), null, "Jr. Los Cipreses 210", true, true);
        este.complete(finished);
        tripRepository.save(este);

        var incident = new Incident(SeedIds.ORGANIZATION, SeedIds.TRIP_ESTE, SeedIds.ROUTE_ESTE,
                "Ruta Este — San Juan de Lurigancho", IncidentType.RETRASO, IncidentSeverity.LOW,
                "Tráfico intenso en la Av. Próceres de la Independencia", "Luis Torres",
                finished.minus(20, ChronoUnit.MINUTES));
        incident.resolve();
        incidentRepository.save(incident);
        LOGGER.info("Trip demo data ensured.");
    }
}
