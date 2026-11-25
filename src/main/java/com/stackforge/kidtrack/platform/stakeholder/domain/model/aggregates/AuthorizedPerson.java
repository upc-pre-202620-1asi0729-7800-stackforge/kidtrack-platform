package com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** A person registered to receive a student. The student is only handed over to one of these people. */
@Entity
@Table(name = "authorized_persons", uniqueConstraints = @UniqueConstraint(name = "uk_authorized_child_document",
        columnNames = {"child_id", "document_number"}))
public class AuthorizedPerson extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(nullable = false, length = 50)
    private String relationship;

    @Column(length = 20)
    private String phone;

    protected AuthorizedPerson() {
    }

    public AuthorizedPerson(UUID organizationId, UUID childId, String fullName, String documentNumber,
                            String relationship, String phone) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.childId = childId;
        this.fullName = required(fullName, 150, "The name of the authorized person");
        this.documentNumber = required(documentNumber, 20, "The document number");
        this.relationship = required(relationship, 50, "The relationship");
        this.phone = phone == null ? "" : phone.trim();
        if (this.phone.length() > 20) {
            throw new IllegalArgumentException("The phone number is too long.");
        }
    }

    private static String required(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        var text = value.trim();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long.");
        }
        return text;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getChildId() {
        return childId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public String getPhone() {
        return phone;
    }
}
