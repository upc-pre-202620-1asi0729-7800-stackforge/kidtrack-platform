package com.stackforge.kidtrack.platform.stakeholder.application.internal.queryservices;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildrenQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriverByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriversQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentsQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderQueryService;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ChildRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.DriverRepository;
import com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories.ParentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StakeholderQueryServiceImpl implements StakeholderQueryService {

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final DriverRepository driverRepository;

    public StakeholderQueryServiceImpl(ParentRepository parentRepository, ChildRepository childRepository,
                                       DriverRepository driverRepository) {
        this.parentRepository = parentRepository;
        this.childRepository = childRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public List<Parent> handle(GetParentsQuery query) {
        return query.organizationId() == null
                ? parentRepository.findAll() : parentRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    public Optional<Parent> handle(GetParentByIdQuery query) {
        return parentRepository.findById(query.parentId());
    }

    @Override
    public List<Child> handle(GetChildrenQuery query) {
        if (query.organizationId() == null) {
            return query.parentId() == null ? childRepository.findAll() : childRepository.findByParentId(query.parentId());
        }
        return query.parentId() == null
                ? childRepository.findByOrganizationId(query.organizationId())
                : childRepository.findByOrganizationIdAndParentId(query.organizationId(), query.parentId());
    }

    @Override
    public Optional<Child> handle(GetChildByIdQuery query) {
        return childRepository.findById(query.childId());
    }

    @Override
    public List<Driver> handle(GetDriversQuery query) {
        return query.organizationId() == null
                ? driverRepository.findAll() : driverRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    public Optional<Driver> handle(GetDriverByIdQuery query) {
        return driverRepository.findById(query.driverId());
    }
}
