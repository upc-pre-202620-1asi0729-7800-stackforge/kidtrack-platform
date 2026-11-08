package com.stackforge.kidtrack.platform.stakeholder.domain.services;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateParentCommand;

/** Write side of the Stakeholder bounded context. */
public interface StakeholderCommandService {

    Parent handle(CreateParentCommand command);

    Parent handle(UpdateParentCommand command);

    void handle(DeleteParentCommand command);

    Child handle(CreateChildCommand command);

    Child handle(UpdateChildCommand command);

    void handle(DeleteChildCommand command);

    Driver handle(CreateDriverCommand command);

    Driver handle(UpdateDriverCommand command);

    void handle(DeleteDriverCommand command);
}
