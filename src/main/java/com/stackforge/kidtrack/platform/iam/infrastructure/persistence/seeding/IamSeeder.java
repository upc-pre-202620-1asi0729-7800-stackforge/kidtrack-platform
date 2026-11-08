package com.stackforge.kidtrack.platform.iam.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.iam.application.internal.outboundservices.HashingService;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.OrganizationStatus;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Email;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the demo organization and accounts on an empty database. It is the first seeder to run because the
 * other bounded contexts reference the organization and the users it creates.
 *
 * <p>Demo credentials: admin@kidtrack.pe / admin123, driver@kidtrack.pe / driver123,
 * parent@kidtrack.pe / parent123.</p>
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamSeeder.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final HashingService hashingService;

    public IamSeeder(OrganizationRepository organizationRepository, UserRepository userRepository,
                     HashingService hashingService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.hashingService = hashingService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!organizationRepository.existsById(SeedIds.ORGANIZATION)) {
            organizationRepository.save(new Organization(SeedIds.ORGANIZATION,
                    "Transportes Escolares Lima Norte S.A.C.", OrganizationStatus.ACTIVE));
        }
        addUser(SeedIds.USER_ADMIN, "Nickolas", "Quispe", "admin@kidtrack.pe", "999111222", "admin123", RoleTier.ADMIN);
        addUser(SeedIds.USER_DRIVER_CARLOS, "Carlos", "Ramirez", "driver@kidtrack.pe", "987654321", "driver123",
                RoleTier.DRIVER);
        addUser(SeedIds.USER_PARENT_ROSITA, "Rosita", "Nery", "parent@kidtrack.pe", "999888777", "parent123",
                RoleTier.PARENT);
        addUser(SeedIds.USER_DRIVER_LUIS, "Luis", "Torres", "luis.torres@kidtrack.pe", "987112233", "driver123",
                RoleTier.DRIVER);
        addUser(SeedIds.USER_PARENT_JORGE, "Jorge", "Quispe", "jorge.quispe@kidtrack.pe", "998877665", "parent123",
                RoleTier.PARENT);
        LOGGER.info("IAM demo data ensured.");
    }

    private void addUser(UUID id, String firstName, String lastName, String email, String phone, String password,
                         RoleTier role) {
        var address = new Email(email);
        if (userRepository.existsByEmailValue(address.value())) {
            return;
        }
        userRepository.save(new User(id, SeedIds.ORGANIZATION, new FullName(firstName, lastName), address, phone,
                hashingService.hash(password), role));
    }
}
