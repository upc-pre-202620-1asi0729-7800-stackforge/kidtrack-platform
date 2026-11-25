package com.stackforge.kidtrack.platform;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end tests of the negative scenarios: a student is only handed over to a registered person, and a force
 * majeure emergency keeps the parents informed until it is closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SadPathApiTests {

    private static final String ORG = SeedIds.ORGANIZATION.toString();
    private static final String TRIP = SeedIds.TRIP_NORTE.toString();
    private static final String LUCIA = SeedIds.CHILD_LUCIA.toString();
    private static final String DIEGO = SeedIds.CHILD_DIEGO.toString();
    private static final String MATEO = SeedIds.CHILD_MATEO.toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authorizedPersonsAreManagedByAdministrators() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");
        var driver = login("driver@kidtrack.pe", "driver123");
        var parent = login("parent@kidtrack.pe", "parent123");
        var body = Map.of("childId", MATEO, "fullName", "Elena Quispe", "documentNumber", "45678912",
                "relationship", "Tía", "phone", "911222333", "organizationId", ORG);

        send(post("/api/v1/authorized-persons"), driver, body).andExpect(status().isForbidden());
        var created = send(post("/api/v1/authorized-persons"), admin, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Elena Quispe"))
                .andReturn().getResponse().getContentAsString();
        send(post("/api/v1/authorized-persons"), admin, body).andExpect(status().isConflict());

        fetch(get("/api/v1/authorized-persons").param("childId", MATEO), driver)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        fetch(get("/api/v1/authorized-persons"), parent).andExpect(status().isForbidden());

        var id = JsonPath.<String>read(created, "$.id");
        fetch(delete("/api/v1/authorized-persons/" + id), admin).andExpect(status().isNoContent());
        fetch(get("/api/v1/authorized-persons").param("childId", MATEO), admin)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aStudentIsOnlyHandedOverToARegisteredPerson() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");
        var driver = login("driver@kidtrack.pe", "driver123");
        var grandmother = authorizedPerson(admin, LUCIA, "María Nery");
        var mateosFather = authorizedPerson(admin, MATEO, "Jorge Quispe");

        // Nothing can be handed over before the trip starts.
        send(post("/api/v1/handovers"), driver, handover(LUCIA, grandmother)).andExpect(status().isBadRequest());
        startTrip(driver);

        send(post("/api/v1/handovers"), login("parent@kidtrack.pe", "parent123"), handover(LUCIA, grandmother))
                .andExpect(status().isForbidden());
        send(post("/api/v1/handovers"), login("luis.torres@kidtrack.pe", "driver123"), handover(LUCIA, grandmother))
                .andExpect(status().isForbidden());
        // A person registered for another student is not valid.
        send(post("/api/v1/handovers"), driver, handover(LUCIA, mateosFather)).andExpect(status().isBadRequest());

        send(post("/api/v1/handovers"), driver,
                Map.of("tripId", TRIP, "childId", LUCIA, "receiverName", "Un vecino"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("NOT_DELIVERED"))
                .andExpect(jsonPath("$.receiverName").value("Un vecino"));
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$[0].type").value("NO_ENTREGADO"))
                .andExpect(jsonPath("$[0].message").value(containsString("Lucía Nery no fue entregado")));

        send(post("/api/v1/handovers"), driver, handover(LUCIA, grandmother))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("DELIVERED"))
                .andExpect(jsonPath("$.receiverName").value("María Nery"))
                .andExpect(jsonPath("$.relationship").value("Abuela"));
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$[0].type").value("ENTREGA"))
                .andExpect(jsonPath("$[0].message")
                        .value("Lucía Nery fue recibido(a) por María Nery (Abuela)."));
        send(post("/api/v1/handovers"), driver, handover(LUCIA, grandmother)).andExpect(status().isConflict());

        fetch(get("/api/v1/handovers").param("tripId", TRIP), admin)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void anEmergencyKeepsTheParentsInformedUntilItIsClosed() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");
        var driver = login("driver@kidtrack.pe", "driver123");
        var rositaBefore = countNotifications(admin, SeedIds.PARENT_ROSITA);
        var jorgeBefore = countNotifications(admin, SeedIds.PARENT_JORGE);
        var emergency = Map.of("tripId", TRIP, "description", "Un alumno se sintió mal", "emergencyType", "MEDICA",
                "location", "Av. Universitaria", "studentIds", List.of(LUCIA, DIEGO), "organizationId", ORG);

        send(post("/api/v1/incidents"), driver, emergency).andExpect(status().isBadRequest());
        startTrip(driver);
        send(post("/api/v1/incidents"), login("luis.torres@kidtrack.pe", "driver123"), emergency)
                .andExpect(status().isForbidden());

        var declared = send(post("/api/v1/incidents"), driver, emergency)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EMERGENCIA"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.serviceName").value("el SAMU"))
                .andExpect(jsonPath("$.servicePhone").value("106"))
                .andExpect(jsonPath("$.studentIds.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        var incidentId = JsonPath.<String>read(declared, "$.id");
        send(post("/api/v1/incidents"), driver, emergency).andExpect(status().isConflict());

        // One message for the parent of the two students aboard, without claiming they are fine yet.
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$[0].type").value("EMERGENCIA"))
                .andExpect(jsonPath("$[0].message").value(allOf(containsString("Lucía Nery y Diego Nery"),
                        containsString("Estamos confirmando"), containsString("Av. Universitaria"),
                        not(containsString("están bien")))));
        notificationsOf(admin, SeedIds.PARENT_JORGE).andExpect(jsonPath("$.length()").value(jorgeBefore));

        fetch(post("/api/v1/incidents/" + incidentId + "/service-calls"), driver)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceContactedAt").isNotEmpty());
        fetch(post("/api/v1/incidents/" + incidentId + "/service-calls"), admin)
                .andExpect(status().isBadRequest());
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$[0].message").value(containsString("Ya se contactó al SAMU (106)")));

        send(post("/api/v1/incidents/" + incidentId + "/updates"), admin,
                Map.of("message", "La ambulancia ya llegó", "studentsSafe", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updates.length()").value(1))
                .andExpect(jsonPath("$.studentsSafe").value(true));
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$[0].type").value("ACTUALIZACION"))
                .andExpect(jsonPath("$[0].message").value(allOf(containsString("La ambulancia ya llegó."),
                        containsString("están bien"))));

        // Only an administrator closes the emergency.
        send(patch("/api/v1/incidents/" + incidentId), driver, Map.of("status", "RESOLVED"))
                .andExpect(status().isForbidden());
        send(patch("/api/v1/incidents/" + incidentId), admin,
                Map.of("status", "RESOLVED", "resolution", "Los alumnos fueron atendidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.updates.length()").value(1));
        notificationsOf(admin, SeedIds.PARENT_ROSITA)
                .andExpect(jsonPath("$.length()").value(rositaBefore + 4))
                .andExpect(jsonPath("$[0].message").value(containsString("fue cerrado. Los alumnos fueron atendidos.")));
        send(post("/api/v1/incidents/" + incidentId + "/updates"), admin, Map.of("message", "Tarde"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOrdinaryIncidentDoesNotAlarmTheParents() throws Exception {
        var admin = login("admin@kidtrack.pe", "admin123");
        var driver = login("driver@kidtrack.pe", "driver123");
        startTrip(driver);
        var rositaBefore = countNotifications(admin, SeedIds.PARENT_ROSITA);

        send(post("/api/v1/incidents"), driver, Map.of("tripId", TRIP, "type", "AVERIA", "severity", "LOW",
                "description", "Llanta baja", "organizationId", ORG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emergencyType").doesNotExist());
        notificationsOf(admin, SeedIds.PARENT_ROSITA).andExpect(jsonPath("$.length()").value(rositaBefore));
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    private void startTrip(String driver) throws Exception {
        send(patch("/api/v1/trips/" + TRIP), driver, Map.of("status", "EN_ROUTE")).andExpect(status().isOk());
    }

    private int countNotifications(String token, UUID parentId) throws Exception {
        var response = notificationsOf(token, parentId).andReturn().getResponse().getContentAsString();
        return JsonPath.<List<Object>>read(response, "$").size();
    }

    private static Map<String, String> handover(String childId, String authorizedPersonId) {
        return Map.of("tripId", TRIP, "childId", childId, "authorizedPersonId", authorizedPersonId);
    }

    private String authorizedPerson(String admin, String childId, String name) throws Exception {
        var response = fetch(get("/api/v1/authorized-persons").param("childId", childId), admin)
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.<List<String>>read(response, "$[?(@.fullName == '" + name + "')].id").get(0);
    }

    private org.springframework.test.web.servlet.ResultActions notificationsOf(String token, UUID parentId)
            throws Exception {
        return fetch(get("/api/v1/notifications").param("parentId", parentId.toString()), token)
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions fetch(MockHttpServletRequestBuilder builder,
                                                                    String token) throws Exception {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions send(MockHttpServletRequestBuilder builder,
                                                                   String token, Object body) throws Exception {
        return fetch(builder.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), token);
    }

    private String login(String email, String password) throws Exception {
        var response = mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
