package com.stackforge.kidtrack.platform.stakeholder.interfaces.acl;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
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

    public StakeholderContextFacadeImpl(DriverRepository driverRepository, ChildRepository childRepository) {
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
    public Optional<String> childName(UUID childId) {
        if (childId == null) {
            return Optional.empty();
        }
        return childRepository.findById(childId).map(Child::getName);
    }
}
