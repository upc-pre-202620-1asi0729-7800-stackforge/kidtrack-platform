package com.stackforge.kidtrack.platform.stakeholder.interfaces.acl;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.AuthorizedPersonRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ChildRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.DriverRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StakeholderContextFacadeImpl implements StakeholderContextFacade {

    private final DriverRepository driverRepository;
    private final ChildRepository childRepository;

    private final AuthorizedPersonRepository authorizedPersonRepository;

    public StakeholderContextFacadeImpl(DriverRepository driverRepository, ChildRepository childRepository,
                                        AuthorizedPersonRepository authorizedPersonRepository) {
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.driverRepository = driverRepository;
        this.childRepository = childRepository;
    }

    @Override
    public Optional<String> driverFullName(UUID userOrDriverId) {
        if (userOrDriverId == null) {
            return Optional.empty();
        }
        return driverRepository.findByUserId(userOrDriverId)
                .or(() -> driverRepository.findById(userOrDriverId))
                .map(Driver::getFullName)
                .map(Object::toString);
    }

    @Override
    public Optional<ChildContact> childContact(UUID childId) {
        if (childId == null) {
            return Optional.empty();
        }
        return childRepository.findById(childId)
                .map(child -> new ChildContact(child.getId(), child.getName(), child.getParentId()));
    }

    @Override
    public Optional<Receiver> authorizedPerson(UUID authorizedPersonId) {
        if (authorizedPersonId == null) {
            return Optional.empty();
        }
        return authorizedPersonRepository.findById(authorizedPersonId).map(person -> new Receiver(person.getId(),
                person.getChildId(), person.getFullName(), person.getRelationship()));
    }

    @Override
    public Optional<String> childName(UUID childId) {
        if (childId == null) {
            return Optional.empty();
        }
        return childRepository.findById(childId).map(Child::getName);
    }
}
