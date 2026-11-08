package com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.valueobjects.BoardingStatus;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ChildRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ParentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads two drivers, two parents and their students for the demo organization. */
@Component
@Order(3)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StakeholderSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StakeholderSeeder.class);

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final DriverRepository driverRepository;

    public StakeholderSeeder(ParentRepository parentRepository, ChildRepository childRepository,
                             DriverRepository driverRepository) {
        this.parentRepository = parentRepository;
        this.childRepository = childRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (driverRepository.count() == 0) {
            driverRepository.save(new Driver(SeedIds.DRIVER_CARLOS, SeedIds.ORGANIZATION, SeedIds.USER_DRIVER_CARLOS,
                    new FullName("Carlos", "Ramirez"), "987654321", "Q12345678", SeedIds.VEHICLE_ABC, true));
            driverRepository.save(new Driver(SeedIds.DRIVER_LUIS, SeedIds.ORGANIZATION, SeedIds.USER_DRIVER_LUIS,
                    new FullName("Luis", "Torres"), "987112233", "Q87654321", SeedIds.VEHICLE_DEF, true));
        }
        if (parentRepository.count() == 0) {
            parentRepository.save(new Parent(SeedIds.PARENT_ROSITA, SeedIds.ORGANIZATION, SeedIds.USER_PARENT_ROSITA,
                    new FullName("Rosita", "Nery"), "parent@kidtrack.pe", "999888777", true));
            parentRepository.save(new Parent(SeedIds.PARENT_JORGE, SeedIds.ORGANIZATION, SeedIds.USER_PARENT_JORGE,
                    new FullName("Jorge", "Quispe"), "jorge.quispe@kidtrack.pe", "998877665", true));
        }
        if (childRepository.count() == 0) {
            childRepository.save(new Child(SeedIds.CHILD_LUCIA, SeedIds.ORGANIZATION, SeedIds.PARENT_ROSITA,
                    "Lucía Nery", "3° Primaria", true, BoardingStatus.EN_ESPERA, false));
            childRepository.save(new Child(SeedIds.CHILD_DIEGO, SeedIds.ORGANIZATION, SeedIds.PARENT_ROSITA,
                    "Diego Nery", "1° Primaria", true, BoardingStatus.EN_ESPERA, false));
            childRepository.save(new Child(SeedIds.CHILD_MATEO, SeedIds.ORGANIZATION, SeedIds.PARENT_JORGE,
                    "Mateo Quispe", "2° Primaria", true, BoardingStatus.EN_ESPERA, false));
        }
        LOGGER.info("Stakeholder demo data ensured.");
    }
}
