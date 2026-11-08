package com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.entities.Stop;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteStatus;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteType;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.VehicleStatus;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.RouteRepository;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Coordinates;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads two vehicles and two routes (a pick-up and a return) of the demo organization. */
@Component
@Order(4)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FleetSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FleetSeeder.class);

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;

    public FleetSeeder(VehicleRepository vehicleRepository, RouteRepository routeRepository) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (vehicleRepository.count() == 0) {
            vehicleRepository.save(new Vehicle(SeedIds.VEHICLE_ABC, SeedIds.ORGANIZATION, "ABC-123",
                    "Mercedes-Benz Sprinter 416", 20, VehicleStatus.ACTIVE));
            vehicleRepository.save(new Vehicle(SeedIds.VEHICLE_DEF, SeedIds.ORGANIZATION, "DEF-456",
                    "Toyota Hiace", 15, VehicleStatus.ACTIVE));
        }
        if (routeRepository.count() == 0) {
            var lucia = SeedIds.CHILD_LUCIA.toString();
            var diego = SeedIds.CHILD_DIEGO.toString();
            var mateo = SeedIds.CHILD_MATEO.toString();

            var norte = new Route(SeedIds.ROUTE_NORTE, SeedIds.ORGANIZATION, "Ruta Norte — Comas / Los Olivos",
                    RouteType.OUTBOUND);
            norte.replaceConfiguration(norte.getName(), RouteType.OUTBOUND, RouteStatus.ACTIVE,
                    SeedIds.USER_DRIVER_CARLOS, "Carlos Ramirez", SeedIds.VEHICLE_ABC, "ABC-123",
                    List.of(lucia, diego, mateo), "06:00", List.of(
                            new Stop(1, "Av. Universitaria cdra. 54", new Coordinates(-11.9496, -77.0622), List.of(lucia)),
                            new Stop(2, "Av. Angélica Gamarra cdra. 8", new Coordinates(-11.9951, -77.0689), List.of(diego)),
                            new Stop(3, "Jr. Las Orquídeas 342", new Coordinates(-11.9887, -77.0745), List.of(mateo)),
                            new Stop(4, "I.E. San Marcos (llegada)", new Coordinates(-12.0030, -77.0610), List.of())));
            routeRepository.save(norte);

            var este = new Route(SeedIds.ROUTE_ESTE, SeedIds.ORGANIZATION, "Ruta Este — San Juan de Lurigancho",
                    RouteType.RETURN);
            este.replaceConfiguration(este.getName(), RouteType.RETURN, RouteStatus.ACTIVE,
                    SeedIds.USER_DRIVER_LUIS, "Luis Torres", SeedIds.VEHICLE_DEF, "DEF-456",
                    List.of(lucia, diego), "14:30", List.of(
                            new Stop(1, "I.E. San Marcos (salida)", new Coordinates(-12.0030, -77.0610), List.of()),
                            new Stop(2, "Av. Próceres de la Independencia 1520", new Coordinates(-12.0140, -77.0120),
                                    List.of(lucia)),
                            new Stop(3, "Jr. Los Cipreses 210", new Coordinates(-12.0071, -77.0033), List.of(diego))));
            routeRepository.save(este);
        }
        LOGGER.info("Fleet demo data ensured.");
    }
}
