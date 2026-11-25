package com.stackforge.kidtrack.platform.notifications.application.internal.eventhandlers;

import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.EmergencyNotice;
import java.util.List;

/**
 * Wording of the messages sent to the parents. They are meant to inform and reassure: each one says how the
 * children are, who is with them and what is being done. They never state that the children are fine unless the
 * driver confirmed it.
 */
final class ParentMessages {

    private static final int MAX_LENGTH = 500;

    private ParentMessages() {
    }

    static String handedOver(String child, String receiver, String relationship) {
        var who = relationship == null || relationship.isBlank() ? receiver : receiver + " (" + relationship + ")";
        return fit(child + " fue recibido(a) por " + who + ".");
    }

    static String notDelivered(String child, String driver, String plate) {
        return fit(child + " no fue entregado(a): no había una persona registrada para recibirlo(a). Sigue con el "
                + "conductor " + driver + vehicle(plate) + ".");
    }

    static String emergency(EmergencyNotice notice, List<String> children) {
        var names = String.join(" y ", children);
        var several = children.size() > 1;
        var care = "Conductor: " + notice.driverName() + vehicle(notice.vehiclePlate()) + ".";
        var state = notice.studentsSafe() == null ? "Estamos confirmando el estado de " + names + "."
                : notice.studentsSafe()
                ? names + (several ? " están bien" : " está bien") + ", según confirmó el conductor."
                : names + (several ? " requieren" : " requiere") + " atención; el equipo está con "
                + (several ? "ellos." : "su hijo(a).");
        // "a el" is contracted to "al" in Spanish
        var toService = (notice.serviceName().startsWith("el ")
                ? "al " + notice.serviceName().substring(3) : "a " + notice.serviceName())
                + " (" + notice.servicePhone() + ")";
        return fit(switch (notice.stage()) {
            case DECLARED -> "Se reportó un caso de " + notice.emergencyLabel() + place(notice.location()) + ". "
                    + state + " " + care + " Se va a contactar " + toService
                    + ". Le avisaremos apenas haya novedades.";
            case SERVICE_CONTACTED -> "Ya se contactó " + toService + " por el caso de " + notice.emergencyLabel()
                    + ". " + state + " " + care;
            case UPDATED -> "Actualización: " + sentence(notice.detail()) + " " + state;
            case RESOLVED -> "El caso de " + notice.emergencyLabel() + " fue cerrado."
                    + (blank(notice.detail()) ? "" : " " + sentence(notice.detail()));
        });
    }

    private static String sentence(String text) {
        var value = text.trim();
        return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") ? value : value + ".";
    }

    private static String vehicle(String plate) {
        return blank(plate) ? "" : ", vehículo " + plate;
    }

    private static String place(String location) {
        return blank(location) ? "" : " en " + location;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String fit(String message) {
        return message.length() <= MAX_LENGTH ? message : message.substring(0, MAX_LENGTH - 1) + "…";
    }
}
