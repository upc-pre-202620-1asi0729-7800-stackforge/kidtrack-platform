package com.stackforge.kidtrack.platform.fleet.domain.model.entities;

import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Coordinates;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A stop of a route: where the bus stops, in which order, and which students get on or off there. */
@Entity
@Table(name = "route_stops")
public class Stop {

    @Id
    private UUID id;

    /** 1-based position of the stop within its route. */
    @Column(name = "stop_position", nullable = false)
    private int position;

    @Column(nullable = false, length = 200)
    private String name;

    @Embedded
    private Coordinates coordinates;

    @Convert(converter = StringListConverter.class)
    @Column(name = "student_ids", columnDefinition = "TEXT")
    private List<String> studentIds = new ArrayList<>();

    protected Stop() {
    }

    public Stop(int position, String name, Coordinates coordinates, List<String> studentIds) {
        if (position < 1) {
            throw new IllegalArgumentException("Stop order must be a positive 1-based position.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name of the stop cannot be empty.");
        }
        this.id = UUID.randomUUID();
        this.position = position;
        this.name = name.trim();
        this.coordinates = coordinates;
        this.studentIds = studentIds == null ? new ArrayList<>() : new ArrayList<>(studentIds);
    }

    public boolean isFirst() {
        return position == 1;
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public List<String> getStudentIds() {
        return studentIds;
    }
}
