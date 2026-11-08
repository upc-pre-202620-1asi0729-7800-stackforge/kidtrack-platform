package com.stackforge.kidtrack.platform.subscription.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreatePlanCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetAllPlansQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetPlanByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionCommandService;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionQueryService;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.CreatePlanResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.PlanResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.transform.SubscriptionResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Commercial plans")
public class PlansController {

    private final SubscriptionCommandService commandService;
    private final SubscriptionQueryService queryService;

    public PlansController(SubscriptionCommandService commandService, SubscriptionQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the plans", description = "Public, ordered from the cheapest to the most expensive.")
    public List<PlanResource> getPlans() {
        return queryService.handle(new GetAllPlansQuery()).stream()
                .map(SubscriptionResourceAssembler::toResource).toList();
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a plan by id")
    public PlanResource getPlanById(@PathVariable UUID planId) {
        var plan = queryService.handle(new GetPlanByIdQuery(planId))
                .orElseThrow(() -> new NotFoundException("Plan was not found."));
        return SubscriptionResourceAssembler.toResource(plan);
    }

    @PostMapping
    @Operation(summary = "Create a plan")
    public ResponseEntity<PlanResource> createPlan(@Valid @RequestBody CreatePlanResource resource,
                                                   @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        var plan = commandService.handle(new CreatePlanCommand(resource.planTier(), resource.name(),
                resource.maxRoutes(), resource.maxDrivers(), resource.maxStudents(),
                resource.price() == null ? BigDecimal.ZERO : resource.price(), resource.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResourceAssembler.toResource(plan));
    }
}
