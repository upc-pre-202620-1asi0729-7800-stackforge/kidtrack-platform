package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** REST resources of the Stakeholder bounded context. Identifiers travel as text, like in the web application. */
public final class StakeholderResources {

    private StakeholderResources() {
    }

    public record ParentResource(String id, String name, String email, String phone, boolean status,
                                 String organizationId) {
    }

    public record ParentRequestResource(@NotBlank String name, String email, String phone, Boolean status,
                                        String organizationId) {
    }

    public record ChildResource(String id, String name, String grade, String parentId, boolean status,
                                String boardingStatus, boolean hasPhoto, String organizationId) {
    }

    public record ChildRequestResource(@NotBlank String name, String grade, String parentId, Boolean status,
                                       String boardingStatus, Boolean hasPhoto, String organizationId) {
    }

    /** Partial update of a student, used to record the boarding status during a trip. */
    public record ChildPatchResource(String name, String grade, String parentId, Boolean status,
                                     String boardingStatus, Boolean hasPhoto) {
    }

    /** A driver or a parent seen as a person profile ({@code role} is {@code driver} or {@code parent}). */
    public record ProfileResource(String id, String userId, String firstName, String lastName, String phone,
                                  String role, String license, String vehicleId, String status,
                                  String organizationId) {
    }

    public record ProfileRequestResource(String userId, String firstName, String lastName, String phone, String role,
                                         String license, String vehicleId, String status, String organizationId) {
    }
}
