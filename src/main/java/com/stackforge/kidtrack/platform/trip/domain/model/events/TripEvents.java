package com.stackforge.kidtrack.platform.trip.domain.model.events;

import java.util.List;
import java.util.UUID;

/** Domain events published by the Trip bounded context. */
public final class TripEvents {

    private TripEvents() {
    }

    /** A trip was deleted; the notifications that refer to it are no longer meaningful. */
    public record TripDeleted(UUID tripId) {
    }

    /** A student was received by a registered person. */
    public record StudentHandedOver(UUID organizationId, UUID tripId, UUID childId, String receiverName,
                                    String relationship) {
    }

    /** A student was not handed over because nobody registered was there. */
    public record StudentNotDelivered(UUID organizationId, UUID tripId, UUID childId, String driverName,
                                      String vehiclePlate) {
    }

    /** What happened to an emergency. */
    public enum EmergencyStage {
        DECLARED,
        SERVICE_CONTACTED,
        UPDATED,
        RESOLVED
    }

    /** News about a force majeure emergency that the parents of the students aboard must receive. */
    public record EmergencyNotice(EmergencyStage stage, UUID organizationId, UUID tripId, List<String> studentIds,
                                  String emergencyLabel, String serviceName, String servicePhone, String driverName,
                                  String vehiclePlate, String location, Boolean studentsSafe, String detail) {
    }
}
