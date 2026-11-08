package com.stackforge.kidtrack.platform.stakeholder.domain.model.queries;

import java.util.UUID;

/** Queries handled by the Stakeholder bounded context. */
public final class StakeholderQueries {

    private StakeholderQueries() {
    }

    public record GetParentsQuery(UUID organizationId) {
    }

    public record GetParentByIdQuery(UUID parentId) {
    }

    /** Lists the students of an organization, optionally those of a single parent. */
    public record GetChildrenQuery(UUID organizationId, UUID parentId) {
    }

    public record GetChildByIdQuery(UUID childId) {
    }

    public record GetDriversQuery(UUID organizationId) {
    }

    public record GetDriverByIdQuery(UUID driverId) {
    }
}
