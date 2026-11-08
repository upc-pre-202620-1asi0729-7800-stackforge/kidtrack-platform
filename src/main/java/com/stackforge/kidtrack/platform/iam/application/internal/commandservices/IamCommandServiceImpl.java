package com.stackforge.kidtrack.platform.iam.application.internal.commandservices;

import com.stackforge.kidtrack.platform.iam.application.internal.outboundservices.HashingService;
import com.stackforge.kidtrack.platform.iam.application.internal.outboundservices.TokenService;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.CreateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignInCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignUpCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateUserCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.OrganizationStatus;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import com.stackforge.kidtrack.platform.iam.domain.services.IamCommandService;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.InvalidCredentialsException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Email;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IamCommandServiceImpl implements IamCommandService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public IamCommandServiceImpl(UserRepository userRepository, OrganizationRepository organizationRepository,
                                 HashingService hashingService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Override
    public User handle(SignUpCommand command) {
        var email = new Email(command.email());
        if (userRepository.existsByEmailValue(email.value())) {
            throw new ConflictException("Email address " + email + " is already registered.");
        }
        requireValidPassword(command.password());
        requireOrganizationIfPresent(command.organizationId());
        var user = new User(
                command.organizationId(),
                new FullName(command.firstName(), command.lastName()),
                email,
                null,
                hashingService.hash(command.password()),
                RoleTier.from(command.roleTier()));
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication handle(SignInCommand command) {
        Email email;
        try {
            email = new Email(command.email());
        } catch (IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }
        var user = userRepository.findByEmailValue(email.value())
                .filter(found -> hashingService.matches(command.password(), found.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));
        return new Authentication(user, tokenService.generateToken(user));
    }

    @Override
    public User handle(UpdateUserCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new NotFoundException("User was not found."));
        var current = user.getFullName();
        var fullName = new FullName(
                command.firstName() != null ? command.firstName() : current.firstName(),
                command.lastName() != null ? command.lastName() : current.lastName());
        var email = command.email() != null ? new Email(command.email()) : user.getEmail();
        if (!email.equals(user.getEmail()) && userRepository.existsByEmailValue(email.value())) {
            throw new ConflictException("Email address " + email + " is already registered.");
        }
        user.updateProfile(fullName, email, command.phone() != null ? command.phone() : user.getPhone());
        if (command.roleTier() != null) {
            user.changeRole(RoleTier.from(command.roleTier()));
        }
        if (command.organizationId() != null) {
            requireOrganizationIfPresent(command.organizationId());
            user.assignOrganization(command.organizationId());
        }
        if (command.password() != null && !command.password().isBlank()) {
            requireValidPassword(command.password());
            user.changePassword(hashingService.hash(command.password()));
        }
        return userRepository.save(user);
    }

    @Override
    public Organization handle(CreateOrganizationCommand command) {
        return organizationRepository.save(
                new Organization(command.name(), OrganizationStatus.from(command.status())));
    }

    @Override
    public Organization handle(UpdateOrganizationCommand command) {
        var organization = organizationRepository.findById(command.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization was not found."));
        if (command.name() != null) {
            organization.rename(command.name());
        }
        if (command.status() != null) {
            if (OrganizationStatus.from(command.status()) == OrganizationStatus.ACTIVE) {
                organization.activate();
            } else {
                organization.suspend();
            }
        }
        return organizationRepository.save(organization);
    }

    private void requireOrganizationIfPresent(UUID organizationId) {
        if (organizationId != null && !organizationRepository.existsById(organizationId)) {
            throw new NotFoundException("Organization was not found.");
        }
    }

    private static void requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("The password must have at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }
}
