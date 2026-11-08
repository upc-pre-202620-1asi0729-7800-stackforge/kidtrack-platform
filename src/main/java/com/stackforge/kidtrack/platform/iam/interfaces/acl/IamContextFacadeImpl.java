package com.stackforge.kidtrack.platform.iam.interfaces.acl;

import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUserByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.services.IamQueryService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final IamQueryService queryService;

    public IamContextFacadeImpl(IamQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Optional<String> fullNameOfUser(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return queryService.handle(new GetUserByIdQuery(userId)).map(user -> user.getFullName().toString());
    }
}
