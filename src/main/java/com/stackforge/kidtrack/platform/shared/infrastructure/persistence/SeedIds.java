package com.stackforge.kidtrack.platform.shared.infrastructure.persistence;

import java.util.UUID;

/**
 * Fixed identifiers of the demo data, shared by the seeders of every bounded context so that the
 * references between contexts (organization, users, children, routes...) are stable across restarts.
 */
public final class SeedIds {

    public static final UUID ORGANIZATION = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    public static final UUID USER_ADMIN = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    public static final UUID USER_DRIVER_CARLOS = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    public static final UUID USER_PARENT_ROSITA = UUID.fromString("b0000000-0000-0000-0000-000000000003");
    public static final UUID USER_DRIVER_LUIS = UUID.fromString("b0000000-0000-0000-0000-000000000004");
    public static final UUID USER_PARENT_JORGE = UUID.fromString("b0000000-0000-0000-0000-000000000005");

    public static final UUID PLAN_BASIC = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    public static final UUID PLAN_INTERMEDIATE = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    public static final UUID PLAN_COMPLETE = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    public static final UUID SUBSCRIPTION = UUID.fromString("50000000-0000-0000-0000-000000000001");

    public static final UUID DRIVER_CARLOS = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    public static final UUID DRIVER_LUIS = UUID.fromString("d0000000-0000-0000-0000-000000000002");
    public static final UUID PARENT_ROSITA = UUID.fromString("e0000000-0000-0000-0000-000000000001");
    public static final UUID PARENT_JORGE = UUID.fromString("e0000000-0000-0000-0000-000000000002");
    public static final UUID CHILD_LUCIA = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    public static final UUID CHILD_DIEGO = UUID.fromString("e1000000-0000-0000-0000-000000000002");
    public static final UUID CHILD_MATEO = UUID.fromString("e1000000-0000-0000-0000-000000000003");

    public static final UUID VEHICLE_ABC = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    public static final UUID VEHICLE_DEF = UUID.fromString("a1000000-0000-0000-0000-000000000002");
    public static final UUID ROUTE_NORTE = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    public static final UUID ROUTE_ESTE = UUID.fromString("f0000000-0000-0000-0000-000000000002");
    public static final UUID TRIP_NORTE = UUID.fromString("71000000-0000-0000-0000-000000000001");
    public static final UUID TRIP_ESTE = UUID.fromString("71000000-0000-0000-0000-000000000002");

    private SeedIds() {
    }
}
