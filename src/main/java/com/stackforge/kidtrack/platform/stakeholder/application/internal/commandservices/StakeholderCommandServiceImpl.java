package com.stackforge.kidtrack.platform.stakeholder.application.internal.commandservices;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.valueobjects.BoardingStatus;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderCommandService;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ChildRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ParentRepository;
import com.stackforge.kidtrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StakeholderCommandServiceImpl implements StakeholderCommandService {

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final DriverRepository driverRepository;
    private final SubscriptionContextFacade subscriptionFacade;

    public StakeholderCommandServiceImpl(ParentRepository parentRepository, ChildRepository childRepository,
                                         DriverRepository driverRepository,
                                         SubscriptionContextFacade subscriptionFacade) {
        this.parentRepository = parentRepository;
        this.childRepository = childRepository;
        this.driverRepository = driverRepository;
        this.subscriptionFacade = subscriptionFacade;
    }

    @Override
    public Parent handle(CreateParentCommand command) {
        var parent = new Parent(command.organizationId(), command.userId(), FullName.fromText(command.name()),
                command.email(), command.phone(), command.active() == null || command.active());
        if (!parent.getEmail().isEmpty()
                && parentRepository.existsByOrganizationIdAndEmail(command.organizationId(), parent.getEmail())) {
            throw new ConflictException("A parent with the email " + parent.getEmail() + " already exists.");
        }
        return parentRepository.save(parent);
    }

    @Override
    public Parent handle(UpdateParentCommand command) {
        var parent = findParent(command.parentId());
        parent.update(
                command.name() != null ? FullName.fromText(command.name()) : parent.getFullName(),
                command.email() != null ? command.email() : parent.getEmail(),
                command.phone() != null ? command.phone() : parent.getPhone(),
                command.active() != null ? command.active() : parent.isActive());
        return parentRepository.save(parent);
    }

    /** Deleting a parent keeps the students, which are detached from it. */
    @Override
    public void handle(DeleteParentCommand command) {
        var parent = findParent(command.parentId());
        childRepository.findByParentId(parent.getId()).forEach(child -> {
            child.detachFromParent();
            childRepository.save(child);
        });
        parentRepository.delete(parent);
    }

    @Override
    public Child handle(CreateChildCommand command) {
        subscriptionFacade.activePlanLimits(command.organizationId()).ifPresent(limits -> {
            if (childRepository.countByOrganizationId(command.organizationId()) + 1 > limits.maxStudents()) {
                throw new BusinessRuleException(
                        "The plan of the organization allows at most " + limits.maxStudents() + " students.");
            }
        });
        requireParentIfPresent(command.parentId());
        var child = new Child(command.organizationId(), command.parentId(), command.name(), command.grade(),
                command.active() == null || command.active(), BoardingStatus.from(command.boardingStatus()),
                command.hasPhoto() != null && command.hasPhoto());
        return childRepository.save(child);
    }

    @Override
    public Child handle(UpdateChildCommand command) {
        var child = findChild(command.childId());
        var parentId = command.clearParent() ? null
                : command.parentId() != null ? command.parentId() : child.getParentId();
        requireParentIfPresent(parentId);
        child.update(parentId,
                command.name() != null ? command.name() : child.getName(),
                command.grade() != null ? command.grade() : child.getGrade(),
                command.active() != null ? command.active() : child.isActive(),
                command.hasPhoto() != null ? command.hasPhoto() : child.isHasPhoto());
        if (command.boardingStatus() != null) {
            child.changeBoardingStatus(BoardingStatus.from(command.boardingStatus()));
        }
        return childRepository.save(child);
    }

    @Override
    public void handle(DeleteChildCommand command) {
        childRepository.delete(findChild(command.childId()));
    }

    @Override
    public Driver handle(CreateDriverCommand command) {
        subscriptionFacade.activePlanLimits(command.organizationId()).ifPresent(limits -> {
            if (driverRepository.countByOrganizationId(command.organizationId()) + 1 > limits.maxDrivers()) {
                throw new BusinessRuleException(
                        "The plan of the organization allows at most " + limits.maxDrivers() + " drivers.");
            }
        });
        var driver = new Driver(command.organizationId(), command.userId(),
                new FullName(command.firstName(), command.lastName()), command.phone(), command.licenseNumber(),
                command.vehicleId(), command.active() == null || command.active());
        return driverRepository.save(driver);
    }

    @Override
    public Driver handle(UpdateDriverCommand command) {
        var driver = findDriver(command.driverId());
        var current = driver.getFullName();
        driver.update(
                command.userId() != null ? command.userId() : driver.getUserId(),
                new FullName(command.firstName() != null ? command.firstName() : current.firstName(),
                        command.lastName() != null ? command.lastName() : current.lastName()),
                command.phone() != null ? command.phone() : driver.getPhone(),
                command.licenseNumber() != null ? command.licenseNumber() : driver.getLicenseNumber(),
                command.clearVehicle() ? null
                        : command.vehicleId() != null ? command.vehicleId() : driver.getVehicleId(),
                command.active() != null ? command.active() : driver.isActive());
        return driverRepository.save(driver);
    }

    @Override
    public void handle(DeleteDriverCommand command) {
        driverRepository.delete(findDriver(command.driverId()));
    }

    private Parent findParent(UUID parentId) {
        return parentRepository.findById(parentId).orElseThrow(() -> new NotFoundException("Parent was not found."));
    }

    private Child findChild(UUID childId) {
        return childRepository.findById(childId).orElseThrow(() -> new NotFoundException("Student was not found."));
    }

    private Driver findDriver(UUID driverId) {
        return driverRepository.findById(driverId).orElseThrow(() -> new NotFoundException("Driver was not found."));
    }

    private void requireParentIfPresent(UUID parentId) {
        if (parentId != null && !parentRepository.existsById(parentId)) {
            throw new NotFoundException("Parent was not found.");
        }
    }
}
