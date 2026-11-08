package com.stackforge.kidtrack.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end tests of the REST API against an in-memory database loaded with the demo data. They exercise the
 * contract the KidTrack web application relies on: sign in, plans, vehicles, routes, trips and notifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KidTrackPlatformApplicationTests {

    private static final String ORG = SeedIds.ORGANIZATION.toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void plansArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].planTier").value("BASIC"))
                .andExpect(jsonPath("$[2].planTier").value("COMPLETE"));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles")).andExpect(status().isUnauthorized());
    }

    @Test
    void signInReturnsTheUserAndAToken() throws Exception {
        mockMvc.perform(json(post("/api/v1/authentication/sign-in"),
                        Map.of("email", "admin@kidtrack.pe", "password", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleTier").value("ADMIN"))
                .andExpect(jsonPath("$.organizationId").value(ORG))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void signInWithAWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(json(post("/api/v1/authentication/sign-in"),
                        Map.of("email", "admin@kidtrack.pe", "password", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoDataIsAvailableToTheAdministrator() throws Exception {
        var token = login("admin@kidtrack.pe", "admin123");
        mockMvc.perform(get("/api/v1/vehicles").param("organizationId", ORG).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/routes").param("organizationId", ORG).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].waypoints.length()").isNumber());
        mockMvc.perform(get("/api/v1/profiles").param("role", "driver").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/children").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/v1/subscriptions").param("state", "ACTIVE").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].planTier").value("COMPLETE"));
    }

    @Test
    void anotherOrganizationCannotBeQueried() throws Exception {
        var token = login("admin@kidtrack.pe", "admin123");
        mockMvc.perform(get("/api/v1/routes")
                        .param("organizationId", "a0000000-0000-0000-0000-0000000000ff")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aDriverCannotCreateVehicles() throws Exception {
        var token = login("driver@kidtrack.pe", "driver123");
        mockMvc.perform(json(post("/api/v1/vehicles").header("Authorization", bearer(token)),
                        Map.of("plate", "XYZ-999", "model", "Van", "capacity", 10, "organizationId", ORG)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theFirstAdministratorOfAnOrganizationCanRegisterAnonymously() throws Exception {
        var organization = mockMvc.perform(json(post("/api/v1/organizations"),
                        Map.of("name", "Transportes Test S.A.C.", "status", "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        var organizationId = JsonPath.<String>read(organization, "$.id");

        var admin = Map.of("firstName", "Ana", "lastName", "Ruiz", "email", "ana.ruiz@test.pe",
                "password", "secret123", "roleTier", "ADMIN", "organizationId", organizationId);
        mockMvc.perform(json(post("/api/v1/authentication/sign-up"), admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleTier").value("ADMIN"));

        // Once the organization has an administrator, further users must be registered by an administrator.
        var second = Map.of("firstName", "Eva", "lastName", "Lopez", "email", "eva.lopez@test.pe",
                "password", "secret123", "roleTier", "ADMIN", "organizationId", organizationId);
        mockMvc.perform(json(post("/api/v1/authentication/sign-up"), second)).andExpect(status().isForbidden());

        var token = login("ana.ruiz@test.pe", "secret123");
        var driver = Map.of("firstName", "Raul", "lastName", "Diaz", "email", "raul.diaz@test.pe",
                "password", "secret123", "roleTier", "DRIVER", "organizationId", organizationId);
        mockMvc.perform(json(post("/api/v1/authentication/sign-up").header("Authorization", bearer(token)), driver))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleTier").value("DRIVER"));
        mockMvc.perform(json(post("/api/v1/authentication/sign-up").header("Authorization", bearer(token)), driver))
                .andExpect(status().isConflict());
    }

    @Test
    void routeAndTripLifecycle() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");

        var vehicle = mockMvc.perform(json(post("/api/v1/vehicles").header("Authorization", bearer(admin)),
                        Map.of("plate", "tst-001", "model", "Test Van", "capacity", 12, "status", "ACTIVE",
                                "organizationId", ORG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("TST-001"))
                .andReturn().getResponse().getContentAsString();
        var vehicleId = JsonPath.<String>read(vehicle, "$.id");

        var route = mockMvc.perform(json(post("/api/v1/routes").header("Authorization", bearer(admin)),
                        routeBody(SeedIds.USER_DRIVER_CARLOS.toString(), vehicleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverName").value("Carlos Ramirez"))
                .andExpect(jsonPath("$.vehiclePlate").value("TST-001"))
                .andExpect(jsonPath("$.waypoints.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        var routeId = JsonPath.<String>read(route, "$.id");

        var tripBody = Map.of("routeId", routeId, "scheduledDate", "2099-01-01", "organizationId", ORG);
        var trip = mockMvc.perform(json(post("/api/v1/trips").header("Authorization", bearer(admin)), tripBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.studentsTotal").value(1))
                .andExpect(jsonPath("$.routeName").value("Ruta Test"))
                .andReturn().getResponse().getContentAsString();
        var tripId = JsonPath.<String>read(trip, "$.id");

        mockMvc.perform(json(post("/api/v1/trips").header("Authorization", bearer(admin)), tripBody))
                .andExpect(status().isConflict());

        // Changing the driver of the route moves its pending trips to the new driver.
        mockMvc.perform(json(put("/api/v1/routes/" + routeId).header("Authorization", bearer(admin)),
                        routeBody(SeedIds.USER_DRIVER_LUIS.toString(), vehicleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName").value("Luis Torres"));
        mockMvc.perform(get("/api/v1/trips/" + tripId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName").value("Luis Torres"));

        // Only the driver assigned to the trip can run it.
        var carlos = login("driver@kidtrack.pe", "driver123");
        mockMvc.perform(json(patch("/api/v1/trips/" + tripId).header("Authorization", bearer(carlos)),
                        Map.of("status", "EN_ROUTE"))).andExpect(status().isForbidden());

        var luis = login("luis.torres@kidtrack.pe", "driver123");
        mockMvc.perform(json(patch("/api/v1/trips/" + tripId).header("Authorization", bearer(luis)),
                        Map.of("status", "EN_ROUTE", "startTime", "2099-01-01T06:00:00Z", "currentStop", "A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ROUTE"))
                .andExpect(jsonPath("$.currentStop").value("A"));
        mockMvc.perform(json(patch("/api/v1/trips/" + tripId).header("Authorization", bearer(luis)),
                        Map.of("studentsBoarded", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentsBoarded").value(1));

        var incident = new java.util.HashMap<String, Object>();
        incident.put("tripId", tripId);
        incident.put("type", "RETRASO");
        incident.put("severity", "LOW");
        incident.put("description", "Trafico intenso en la avenida");
        incident.put("organizationId", ORG);
        var reported = mockMvc.perform(json(post("/api/v1/incidents").header("Authorization", bearer(luis)), incident))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeName").value("Ruta Test"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        var incidentId = JsonPath.<String>read(reported, "$.id");
        mockMvc.perform(json(patch("/api/v1/incidents/" + incidentId).header("Authorization", bearer(admin)),
                        Map.of("status", "RESOLVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        var completion = new java.util.HashMap<String, Object>();
        completion.put("status", "COMPLETED");
        completion.put("studentsBoarded", 1);
        completion.put("currentStop", null);
        completion.put("currentLocation", "B");
        mockMvc.perform(json(patch("/api/v1/trips/" + tripId).header("Authorization", bearer(luis)), completion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endTime").isNotEmpty())
                .andExpect(jsonPath("$.currentStop").value(org.hamcrest.Matchers.nullValue()));

        // A notification tied to the trip disappears together with the trip.
        var notification = new java.util.HashMap<String, Object>();
        notification.put("type", "LLEGADA");
        notification.put("message", "El bus llego a destino");
        notification.put("tripId", tripId);
        notification.put("organizationId", ORG);
        var sent = mockMvc.perform(json(post("/api/v1/notifications").header("Authorization", bearer(luis)),
                        notification))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.read").value(false))
                .andReturn().getResponse().getContentAsString();
        var notificationId = JsonPath.<String>read(sent, "$.id");
        mockMvc.perform(json(patch("/api/v1/notifications/" + notificationId).header("Authorization", bearer(admin)),
                        Map.of("read", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(delete("/api/v1/routes/" + routeId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/trips/" + tripId).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/notifications/" + notificationId).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/incidents/" + incidentId).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/vehicles/" + vehicleId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentsCanBeManagedByAdministratorsAndBoardedByDrivers() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");
        var parentId = SeedIds.PARENT_JORGE.toString();

        var child = mockMvc.perform(json(post("/api/v1/children").header("Authorization", bearer(admin)),
                        Map.of("name", "Ana Quispe", "grade", "4° Primaria", "parentId", parentId, "status", true,
                                "organizationId", ORG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boardingStatus").value("EN_ESPERA"))
                .andReturn().getResponse().getContentAsString();
        var childId = JsonPath.<String>read(child, "$.id");

        var driver = login("driver@kidtrack.pe", "driver123");
        mockMvc.perform(json(patch("/api/v1/children/" + childId).header("Authorization", bearer(driver)),
                        Map.of("boardingStatus", "ABORDADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardingStatus").value("ABORDADO"))
                .andExpect(jsonPath("$.name").value("Ana Quispe"));
        mockMvc.perform(json(put("/api/v1/children/" + childId).header("Authorization", bearer(driver)),
                        Map.of("name", "Ana Quispe", "grade", "5° Primaria", "organizationId", ORG)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/children").param("parentId", parentId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(delete("/api/v1/children/" + childId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    private Map<String, Object> routeBody(String driverId, String vehicleId) {
        return Map.ofEntries(
                Map.entry("name", "Ruta Test"),
                Map.entry("type", "OUTBOUND"),
                Map.entry("driverId", driverId),
                Map.entry("driverName", ""),
                Map.entry("vehicleId", vehicleId),
                Map.entry("vehiclePlate", ""),
                Map.entry("studentIds", List.of(SeedIds.CHILD_LUCIA.toString())),
                Map.entry("scheduledStartTime", "07:15"),
                Map.entry("status", "ACTIVE"),
                Map.entry("organizationId", ORG),
                Map.entry("waypoints", List.of(
                        Map.of("order", 1, "name", "Parada A", "lat", -12.0, "lng", -77.0, "studentIds", List.of()),
                        Map.of("order", 2, "name", "Parada B", "lat", -12.1, "lng", -77.1,
                                "studentIds", List.of()))));
    }

    private String login(String email, String password) throws Exception {
        var response = mockMvc.perform(json(post("/api/v1/authentication/sign-in"),
                        Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, Object body)
            throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }
}
