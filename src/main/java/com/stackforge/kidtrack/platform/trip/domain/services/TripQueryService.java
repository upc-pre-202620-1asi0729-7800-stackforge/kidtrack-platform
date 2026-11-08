package com.stackforge.kidtrack.platform.trip.domain.services;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentsQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripsQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the Trip bounded context. */
public interface TripQueryService {

    List<Trip> handle(GetTripsQuery query);

    Optional<Trip> handle(GetTripByIdQuery query);

    List<Incident> handle(GetIncidentsQuery query);

    Optional<Incident> handle(GetIncidentByIdQuery query);
}
