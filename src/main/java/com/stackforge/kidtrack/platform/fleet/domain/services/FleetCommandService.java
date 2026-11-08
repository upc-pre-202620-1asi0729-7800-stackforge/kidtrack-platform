package com.stackforge.kidtrack.platform.fleet.domain.services;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateVehicleCommand;

/** Write side of the Fleet bounded context. */
public interface FleetCommandService {

    Vehicle handle(CreateVehicleCommand command);

    Vehicle handle(UpdateVehicleCommand command);

    void handle(DeleteVehicleCommand command);

    Route handle(CreateRouteCommand command);

    Route handle(UpdateRouteCommand command);

    void handle(DeleteRouteCommand command);
}
