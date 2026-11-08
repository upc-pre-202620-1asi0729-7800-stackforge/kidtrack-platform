package com.stackforge.kidtrack.platform.subscription.domain.services;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreatePlanCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreateSubscriptionCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.UpdateSubscriptionCommand;

/** Write side of the Subscription bounded context. */
public interface SubscriptionCommandService {

    Plan handle(CreatePlanCommand command);

    Subscription handle(CreateSubscriptionCommand command);

    Subscription handle(UpdateSubscriptionCommand command);
}
