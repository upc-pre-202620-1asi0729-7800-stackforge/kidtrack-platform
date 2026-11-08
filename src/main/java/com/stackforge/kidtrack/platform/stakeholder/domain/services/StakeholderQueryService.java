package com.stackforge.kidtrack.platform.stakeholder.domain.services;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildrenQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriverByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriversQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentsQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the Stakeholder bounded context. */
public interface StakeholderQueryService {

    List<Parent> handle(GetParentsQuery query);

    Optional<Parent> handle(GetParentByIdQuery query);

    List<Child> handle(GetChildrenQuery query);

    Optional<Child> handle(GetChildByIdQuery query);

    List<Driver> handle(GetDriversQuery query);

    Optional<Driver> handle(GetDriverByIdQuery query);
}
