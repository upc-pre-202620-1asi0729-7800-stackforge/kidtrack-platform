package com.stackforge.kidtrack.platform.iam.application.internal.queryservices;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationHasAdminQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUserByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUsersQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import com.stackforge.kidtrack.platform.iam.domain.services.IamQueryService;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IamQueryServiceImpl implements IamQueryService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public IamQueryServiceImpl(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public List<User> handle(GetUsersQuery query) {
        return query.organizationId() == null
                ? userRepository.findAll()
                : userRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    public Optional<Organization> handle(GetOrganizationByIdQuery query) {
        return organizationRepository.findById(query.organizationId());
    }

    @Override
    public boolean handle(GetOrganizationHasAdminQuery query) {
        return query.organizationId() != null
                && userRepository.existsByOrganizationIdAndRole(query.organizationId(), RoleTier.ADMIN);
    }
}
