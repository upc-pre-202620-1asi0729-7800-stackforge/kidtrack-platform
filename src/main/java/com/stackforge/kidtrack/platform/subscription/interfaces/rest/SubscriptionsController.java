package com.stackforge.kidtrack.platform.subscription.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreateSubscriptionCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.UpdateSubscriptionCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetAllPlansQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionsQuery;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionCommandService;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionQueryService;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.CreateSubscriptionResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.SubscriptionResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.UpdateSubscriptionResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.transform.SubscriptionResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscriptions of the organizations to the plans")
public class SubscriptionsController {

    private final SubscriptionCommandService commandService;
    private final SubscriptionQueryService queryService;

    public SubscriptionsController(SubscriptionCommandService commandService,
                                   SubscriptionQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the subscriptions",
            description = "Lists the subscriptions of the caller's organization, optionally filtered by state.")
    public List<SubscriptionResource> getSubscriptions(@RequestParam(required = false) String organizationId,
                                                       @RequestParam(required = false) String state,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        var subscriptions = queryService.handle(new GetSubscriptionsQuery(scope, state));
        var plans = plansById();
        return subscriptions.stream()
                .map(subscription -> SubscriptionResourceAssembler.toResource(subscription,
                        plans.get(subscription.getPlanId())))
                .toList();
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get a subscription by id")
    public SubscriptionResource getSubscriptionById(@PathVariable UUID subscriptionId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        var subscription = find(subscriptionId);
        Caller.scope(Caller.from(jwt), subscription.getOrganizationId());
        return toResource(subscription);
    }

    @PostMapping
    @Operation(summary = "Subscribe to a plan",
            description = "Creates an active subscription and cancels the previous active one of the organization.")
    public ResponseEntity<SubscriptionResource> createSubscription(
            @Valid @RequestBody CreateSubscriptionResource resource, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.scope(caller, Ids.parseOrNull(resource.organizationId()));
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required.");
        }
        var subscription = commandService.handle(new CreateSubscriptionCommand(
                organizationId,
                Ids.parse(resource.planId(), "planId"),
                resource.state(),
                Dates.parseInstant(resource.startDate()),
                Dates.parseInstant(resource.endDate())));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResource(subscription));
    }

    @PatchMapping("/{subscriptionId}")
    @Operation(summary = "Update a subscription", description = "Changes the plan, the state or the dates.")
    public SubscriptionResource patchSubscription(@PathVariable UUID subscriptionId,
                                                  @RequestBody UpdateSubscriptionResource resource,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return update(subscriptionId, resource, jwt);
    }

    @PutMapping("/{subscriptionId}")
    @Operation(summary = "Update a subscription")
    public SubscriptionResource putSubscription(@PathVariable UUID subscriptionId,
                                                @RequestBody UpdateSubscriptionResource resource,
                                                @AuthenticationPrincipal Jwt jwt) {
        return update(subscriptionId, resource, jwt);
    }

    private SubscriptionResource update(UUID subscriptionId, UpdateSubscriptionResource resource, Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var existing = find(subscriptionId);
        if (!existing.getOrganizationId().equals(caller.organizationId())) {
            throw new AccessDeniedException("The subscription belongs to another organization.");
        }
        var updated = commandService.handle(new UpdateSubscriptionCommand(
                subscriptionId,
                Ids.parseOrNull(resource.planId()),
                resource.state(),
                Dates.parseInstant(resource.startDate()),
                Dates.parseInstant(resource.endDate())));
        return toResource(updated);
    }

    private Subscription find(UUID subscriptionId) {
        return queryService.handle(new GetSubscriptionByIdQuery(subscriptionId))
                .orElseThrow(() -> new NotFoundException("Subscription was not found."));
    }

    private SubscriptionResource toResource(Subscription subscription) {
        return SubscriptionResourceAssembler.toResource(subscription, plansById().get(subscription.getPlanId()));
    }

    private Map<UUID, Plan> plansById() {
        return queryService.handle(new GetAllPlansQuery()).stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));
    }
}
