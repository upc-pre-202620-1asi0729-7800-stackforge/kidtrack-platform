package com.stackforge.kidtrack.platform.trip.domain.services;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.CreateTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.DeleteTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.ReportIncidentCommand;

/** Write side of the Trip bounded context. */
public interface TripCommandService {

    Trip handle(CreateTripCommand command);

    Trip handle(PatchTripCommand command);

    void handle(DeleteTripCommand command);

    Incident handle(ReportIncidentCommand command);

    Incident handle(PatchIncidentCommand command);
}
