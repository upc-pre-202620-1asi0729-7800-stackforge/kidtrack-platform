package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ChildResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ParentResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ProfileResource;

/** Converts stakeholder aggregates into REST resources. */
public final class StakeholderResourceAssembler {

    public static final String ROLE_DRIVER = "driver";
    public static final String ROLE_PARENT = "parent";

    private StakeholderResourceAssembler() {
    }

    public static ParentResource toResource(Parent parent) {
        return new ParentResource(parent.getId().toString(), parent.getFullName().toString(), parent.getEmail(),
                parent.getPhone(), parent.isActive(), Ids.asText(parent.getOrganizationId()));
    }

    public static ChildResource toResource(Child child) {
        return new ChildResource(child.getId().toString(), child.getName(), child.getGrade(),
                Ids.asText(child.getParentId()), child.isActive(), child.getBoardingStatus().name(),
                child.isHasPhoto(), Ids.asText(child.getOrganizationId()));
    }

    public static ProfileResource toProfile(Driver driver) {
        return new ProfileResource(driver.getId().toString(), Ids.asText(driver.getUserId()),
                driver.getFullName().firstName(), driver.getFullName().lastName(), driver.getPhone(), ROLE_DRIVER,
                driver.getLicenseNumber(), Ids.asText(driver.getVehicleId()), status(driver.isActive()),
                Ids.asText(driver.getOrganizationId()));
    }

    public static ProfileResource toProfile(Parent parent) {
        return new ProfileResource(parent.getId().toString(), Ids.asText(parent.getUserId()),
                parent.getFullName().firstName(), parent.getFullName().lastName(), parent.getPhone(), ROLE_PARENT,
                null, null, status(parent.isActive()), Ids.asText(parent.getOrganizationId()));
    }

    private static String status(boolean active) {
        return active ? "ACTIVE" : "INACTIVE";
    }
}
