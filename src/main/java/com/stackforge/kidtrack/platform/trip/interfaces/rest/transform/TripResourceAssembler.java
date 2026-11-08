package com.stackforge.kidtrack.platform.trip.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.IncidentResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.TripResource;
import java.util.List;

/** Converts trip aggregates into REST resources. */
public final class TripResourceAssembler {

    private TripResourceAssembler() {
    }

    public static TripResource toResource(Trip trip) {
        return new TripResource(
                trip.getId().toString(),
                trip.getRouteId().toString(),
                trip.getRouteName(),
                Ids.asText(trip.getDriverId()),
                trip.getDriverName(),
                Ids.asText(trip.getVehicleId()),
                trip.getVehiclePlate(),
                List.copyOf(trip.getStudentIds()),
                trip.getTripType().name(),
                trip.getScheduledDate(),
                trip.getScheduledStartTime(),
                trip.getStatus().name(),
                Dates.format(trip.getStartTime()),
                Dates.format(trip.getEndTime()),
                trip.getStudentsTotal(),
                trip.getStudentsBoarded(),
                trip.getCurrentStop(),
                trip.getCurrentLocation(),
                trip.getOrganizationId().toString());
    }

    public static IncidentResource toResource(Incident incident) {
        return new IncidentResource(
                incident.getId().toString(),
                Ids.asText(incident.getTripId()),
                Ids.asText(incident.getRouteId()),
                incident.getRouteName(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getDescription(),
                incident.getReportedBy(),
                Dates.format(incident.getReportedAt()),
                incident.getStatus().name(),
                incident.getOrganizationId().toString());
    }
}
