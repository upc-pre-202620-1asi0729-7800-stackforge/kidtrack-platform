package com.stackforge.kidtrack.platform.trip.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.StringListConverter;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One run of a route on a given date. It goes from SCHEDULED to EN_ROUTE (the driver starts it) to COMPLETED, or to
 * CANCELLED. The route, driver and vehicle data are kept with the trip so that its history stays readable even
 * when the route changes later.
 */
@Entity
@Table(name = "trips")
public class Trip extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "route_name", length = 150)
    private String routeName;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 20)
    private TripType tripType;

    /** Day of the trip, ISO yyyy-MM-dd. */
    @Column(name = "scheduled_date", nullable = false, length = 10)
    private String scheduledDate;

    /** Departure time, HH:mm. */
    @Column(name = "scheduled_start_time", length = 5)
    private String scheduledStartTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "students_total", nullable = false)
    private int studentsTotal;

    @Column(name = "students_boarded", nullable = false)
    private int studentsBoarded;

    @Column(name = "current_stop", length = 200)
    private String currentStop;

    @Column(name = "current_location", length = 200)
    private String currentLocation;

    protected Trip() {
    }

    public Trip(UUID organizationId, UUID routeId, String routeName, UUID driverId, String driverName,
                UUID vehicleId, String vehiclePlate, List<String> studentIds, TripType tripType,
                String scheduledDate, String scheduledStartTime) {
        this(UUID.randomUUID(), organizationId, routeId, routeName, driverId, driverName, vehicleId, vehiclePlate,
                studentIds, tripType, scheduledDate, scheduledStartTime);
    }

    /** Creates a trip with a predefined identifier (used by the demo data seeder). */
    public Trip(UUID id, UUID organizationId, UUID routeId, String routeName, UUID driverId, String driverName,
                UUID vehicleId, String vehiclePlate, List<String> studentIds, TripType tripType,
                String scheduledDate, String scheduledStartTime) {
        if (scheduledDate == null || scheduledDate.isBlank()) {
            throw new IllegalArgumentException("The date of the trip is required.");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.routeId = routeId;
        this.routeName = routeName == null ? "" : routeName;
        this.driverId = driverId;
        this.driverName = driverName == null ? "" : driverName;
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate == null ? "" : vehiclePlate;
        this.studentIds = studentIds == null ? new ArrayList<>() : new ArrayList<>(studentIds);
        this.studentsTotal = this.studentIds.size();
        this.tripType = tripType == null ? TripType.OUTBOUND : tripType;
        this.scheduledDate = scheduledDate.trim();
        this.scheduledStartTime = scheduledStartTime == null ? "" : scheduledStartTime.trim();
        this.status = TripStatus.SCHEDULED;
    }

    /** The driver starts the trip. */
    public void start(Instant at, String firstStop) {
        if (status != TripStatus.SCHEDULED) {
            throw new BusinessRuleException("Only a scheduled trip can be started.");
        }
        this.status = TripStatus.EN_ROUTE;
        this.startTime = at == null ? Instant.now() : at;
        this.currentStop = firstStop;
    }

    /** The driver finishes the trip. */
    public void complete(Instant at) {
        if (status != TripStatus.EN_ROUTE) {
            throw new BusinessRuleException("Only a trip that is en route can be completed.");
        }
        this.status = TripStatus.COMPLETED;
        this.endTime = at == null ? Instant.now() : at;
    }

    public void cancel() {
        if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) {
            throw new BusinessRuleException("A finished trip cannot be cancelled.");
        }
        this.status = TripStatus.CANCELLED;
    }

    /** Puts the trip back to its scheduled state, forgetting the progress made. */
    public void reset() {
        if (status == TripStatus.CANCELLED) {
            throw new BusinessRuleException("A cancelled trip cannot be restarted.");
        }
        this.status = TripStatus.SCHEDULED;
        this.startTime = null;
        this.endTime = null;
        this.studentsBoarded = 0;
        this.currentStop = null;
        this.currentLocation = null;
    }

    /** Records where the bus is and how many students are on board. */
    public void updateProgress(Integer boarded, String stop, String location, boolean stopGiven, boolean locationGiven) {
        if (boarded != null) {
            if (boarded < 0 || boarded > studentsTotal) {
                throw new BusinessRuleException("The boarded students must be between 0 and " + studentsTotal + ".");
            }
            this.studentsBoarded = boarded;
        }
        if (stopGiven) {
            this.currentStop = stop;
        }
        if (locationGiven) {
            this.currentLocation = location;
        }
    }

    /** Keeps the trip in line with the route: new driver, vehicle or students. Only open trips follow the route. */
    public void followRoute(UUID driverId, String driverName, UUID vehicleId, String vehiclePlate,
                            List<String> studentIds) {
        if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) {
            throw new BusinessRuleException("A finished trip cannot be reassigned.");
        }
        this.driverId = driverId;
        this.driverName = driverName == null ? "" : driverName;
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate == null ? "" : vehiclePlate;
        this.studentIds = studentIds == null ? new ArrayList<>() : new ArrayList<>(studentIds);
        this.studentsTotal = this.studentIds.size();
        this.studentsBoarded = Math.min(studentsBoarded, studentsTotal);
    }

    public boolean isOpen() {
        return status == TripStatus.SCHEDULED || status == TripStatus.EN_ROUTE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
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

    public TripType getTripType() {
        return tripType;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public String getScheduledStartTime() {
        return scheduledStartTime;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public int getStudentsTotal() {
        return studentsTotal;
    }

    public int getStudentsBoarded() {
        return studentsBoarded;
    }

    public String getCurrentStop() {
        return currentStop;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }
}
