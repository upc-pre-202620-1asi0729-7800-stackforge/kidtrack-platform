package com.stackforge.kidtrack.platform.fleet.domain.model.aggregates;

import com.stackforge.kidtrack.platform.fleet.domain.model.entities.Stop;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteStatus;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteType;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate root of the fleet planning: a school route with its ordered stops, the driver and the vehicle that
 * serve it, the students it carries and the time it leaves.
 */
@Entity
@Table(name = "routes")
public class Route extends AuditableEntity {

    public static final int NAME_MAX_LENGTH = 150;
    private static final Pattern TIME_FORMAT = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStatus status;

    /** The sign in account of the driver that serves the route. */
    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "driver_name", length = 200)
    private String driverName;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    @Convert(converter = StringListConverter.class)
    @Column(name = "student_ids", columnDefinition = "TEXT")
    private List<String> studentIds = new ArrayList<>();

    /** Departure time, HH:mm. */
    @Column(name = "scheduled_start_time", length = 5)
    private String scheduledStartTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    @OrderBy("position ASC")
    private List<Stop> stops = new ArrayList<>();

    protected Route() {
    }

    public Route(UUID organizationId, String name, RouteType type) {
        this(UUID.randomUUID(), organizationId, name, type);
    }

    /** Creates a route with a predefined identifier (used by the demo data seeder). */
    public Route(UUID id, UUID organizationId, String name, RouteType type) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = validateName(name);
        this.type = type == null ? RouteType.OUTBOUND : type;
        this.status = RouteStatus.ACTIVE;
    }

    /** Replaces the whole configuration of the route (the web application edits a route as a whole). */
    public void replaceConfiguration(String name, RouteType type, RouteStatus status, UUID driverId,
                                     String driverName, UUID vehicleId, String vehiclePlate,
                                     List<String> studentIds, String scheduledStartTime, List<Stop> stops) {
        this.name = validateName(name);
        this.type = type == null ? RouteType.OUTBOUND : type;
        this.status = status == null ? RouteStatus.ACTIVE : status;
        this.driverId = driverId;
        this.driverName = driverName == null ? "" : driverName.trim();
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate == null ? "" : vehiclePlate.trim();
        this.studentIds = studentIds == null ? new ArrayList<>() : new ArrayList<>(studentIds);
        this.scheduledStartTime = validateTime(scheduledStartTime);
        this.stops.clear();
        if (stops != null) {
            stops.stream().sorted(Comparator.comparingInt(Stop::getPosition)).forEach(this.stops::add);
        }
    }

    public void activate() {
        this.status = RouteStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = RouteStatus.INACTIVE;
    }

    /** Detaches the vehicle, e.g. because it was removed from the fleet. */
    public void releaseVehicle() {
        this.vehicleId = null;
        this.vehiclePlate = "";
    }

    /** Refreshes the plate kept with the route when the vehicle changes it. */
    public void refreshVehiclePlate(String plate) {
        this.vehiclePlate = plate == null ? "" : plate;
    }

    /** What is still missing for the route to be operable; an empty list means it is complete. */
    public List<String> missingRequirements() {
        var missing = new ArrayList<String>();
        if (driverId == null) {
            missing.add("driver");
        }
        if (vehicleId == null) {
            missing.add("vehicle");
        }
        if (studentIds.isEmpty()) {
            missing.add("students");
        }
        if (stops.size() < 2) {
            missing.add("at least 2 stops");
        }
        if (scheduledStartTime == null || scheduledStartTime.isBlank()) {
            missing.add("departure time");
        }
        return missing;
    }

    public boolean isComplete() {
        return missingRequirements().isEmpty();
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Route name cannot be empty.");
        }
        var value = name.trim();
        if (value.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Route name is too long.");
        }
        return value;
    }

    private static String validateTime(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        var value = time.trim();
        if (!TIME_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("'" + time + "' is not a valid departure time (expected HH:mm).");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public RouteType getType() {
        return type;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public List<String> getStudentIds() {
        return studentIds;
    }

    public String getScheduledStartTime() {
        return scheduledStartTime;
    }

    public List<Stop> getStops() {
        return stops;
    }
}
