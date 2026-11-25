package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** A news item about an emergency that was sent to the parents. */
@Embeddable
public record IncidentUpdate(
        @Column(name = "message", length = 300, nullable = false) String message,
        @Column(name = "sent_at", nullable = false) Instant sentAt,
        @Column(name = "author", length = 200) String author) {
}
