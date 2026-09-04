package com.govmesh.food.service;

import com.govmesh.food.dto.ApplicationDTOs.*;
import com.govmesh.food.dto.RationRecordDTOs.RationRecordDTO;
import com.govmesh.food.entity.Application;
import com.govmesh.food.entity.AuditLog;
import com.govmesh.food.entity.Notification;
import com.govmesh.food.entity.RationRecord;
import com.govmesh.food.exception.ResourceNotFoundException;
import com.govmesh.food.exception.UnauthorizedException;
import com.govmesh.food.repository.ApplicationRepository;
import com.govmesh.food.repository.AuditLogRepository;
import com.govmesh.food.repository.NotificationRepository;
import com.govmesh.food.repository.RationRecordRepository;
import com.govmesh.food.security.UserPrincipal;
import com.govmesh.food.soap.dto.SoapResultDTO;
import com.govmesh.food.soap.dto.UpdateRationAddressCommand;
import com.govmesh.food.soap.exception.SoapServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final RationRecordRepository rationRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              RationRecordRepository rationRecordRepository,
                              AuditLogRepository auditLogRepository,
                              NotificationRepository notificationRepository) {
        this.applicationRepository = applicationRepository;
        this.rationRecordRepository = rationRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<ApplicationDTO> getApplications(String query, String status, String type) {
        List<Application> apps = applicationRepository.filterApplications(query, status, type);
        return apps.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public ApplicationDetailDTO getApplicationById(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        RationRecordDTO rationRecordDTO = null;
        if (app.getRationCardNo() != null) {
            RationRecord record = rationRecordRepository.findByRationCardNo(app.getRationCardNo()).orElse(null);
            if (record != null) {
                rationRecordDTO = mapToRationDTO(record);
            }
        }

        return ApplicationDetailDTO.builder()
                .application(mapToDTO(app))
                .currentRationRecord(rationRecordDTO)
                .build();
    }

    public ApplicationDetailDTO getApplicationByApplicationId(String applicationId) {
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with Application ID: " + applicationId));

        RationRecordDTO rationRecordDTO = null;
        if (app.getRationCardNo() != null) {
            RationRecord record = rationRecordRepository.findByRationCardNo(app.getRationCardNo()).orElse(null);
            if (record != null) {
                rationRecordDTO = mapToRationDTO(record);
            }
        }

        return ApplicationDetailDTO.builder()
                .application(mapToDTO(app))
                .currentRationRecord(rationRecordDTO)
                .build();
    }

    public ApplicationDetailDTO startReview(Long id, UserPrincipal currentUser) {
        verifyNotAuditor(currentUser);

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        if ("APPROVED".equalsIgnoreCase(app.getCurrentStatus()) || "REJECTED".equalsIgnoreCase(app.getCurrentStatus())) {
            throw new IllegalStateException("Application " + app.getApplicationId() + " has already been processed and cannot be reviewed.");
        }

        app.setCurrentStatus("UNDER_REVIEW");
        app.setReviewedByOfficer(currentUser.getEmployeeId() != null ? currentUser.getEmployeeId() : currentUser.getUsername());
        Application saved = applicationRepository.save(app);

        // Audit Log
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(currentUser.getId())
                .action("APPLICATION_REVIEW_STARTED")
                .result("SUCCESS")
                .description("Officer " + currentUser.getUsername() + " started review for application " + app.getApplicationId())
                .build());

        return getApplicationById(saved.getId());
    }

    @Transactional
    public ApplicationDetailDTO approveApplication(Long id, String comments, UserPrincipal currentUser) {
        verifySeniorOrAdmin(currentUser);

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        if ("APPROVED".equalsIgnoreCase(app.getCurrentStatus())) {
            throw new IllegalStateException("Application " + app.getApplicationId() + " has already been approved.");
        }
        if ("REJECTED".equalsIgnoreCase(app.getCurrentStatus())) {
            throw new IllegalStateException("Application " + app.getApplicationId() + " was rejected and cannot be approved directly.");
        }

        // Transactional update of RationRecord if ADDRESS_UPDATE
        if (app.getRationCardNo() != null) {
            RationRecord record = rationRecordRepository.findByRationCardNo(app.getRationCardNo())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked Ration Card Record not found: " + app.getRationCardNo()));

            if (app.getRequestedAddress() != null && !app.getRequestedAddress().isBlank()) {
                record.setHouseAddress(app.getRequestedAddress());
                record.setUpdateStatus("UPDATED");
                rationRecordRepository.save(record);

                auditLogRepository.save(AuditLog.builder()
                        .timestamp(LocalDateTime.now())
                        .applicationId(app.getApplicationId())
                        .officerId(currentUser.getId())
                        .action("RATION_RECORD_UPDATED")
                        .result("SUCCESS")
                        .description("Updated house address for Ration Card " + record.getRationCardNo() + " to: " + app.getRequestedAddress())
                        .build());
            }
        }

        app.setCurrentStatus("APPROVED");
        app.setOfficerComments(comments != null ? comments : "Address update approved after departmental verification.");
        app.setReviewedByOfficer(currentUser.getEmployeeId() != null ? currentUser.getEmployeeId() : currentUser.getUsername());
        Application saved = applicationRepository.save(app);

        // Audit Log
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(currentUser.getId())
                .action("APPLICATION_APPROVED")
                .result("SUCCESS")
                .description("Application " + app.getApplicationId() + " approved by officer " + currentUser.getUsername())
                .build());

        // Notification
        notificationRepository.save(Notification.builder()
                .recipientUserId(currentUser.getId())
                .title("Application Approved")
                .message("Address update request " + app.getApplicationId() + " has been approved successfully.")
                .type("REQUEST")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        return getApplicationById(saved.getId());
    }

    @Transactional
    public ApplicationDetailDTO rejectApplication(Long id, String reason, UserPrincipal currentUser) {
        verifySeniorOrAdmin(currentUser);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        if ("APPROVED".equalsIgnoreCase(app.getCurrentStatus()) || "REJECTED".equalsIgnoreCase(app.getCurrentStatus())) {
            throw new IllegalStateException("Application " + app.getApplicationId() + " is already in terminal state " + app.getCurrentStatus());
        }

        app.setCurrentStatus("REJECTED");
        app.setOfficerComments(reason);
        app.setReviewedByOfficer(currentUser.getEmployeeId() != null ? currentUser.getEmployeeId() : currentUser.getUsername());
        Application saved = applicationRepository.save(app);

        // Audit Log (Ration Record remains UNCHANGED)
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(currentUser.getId())
                .action("APPLICATION_REJECTED")
                .result("SUCCESS")
                .description("Application " + app.getApplicationId() + " rejected. Reason: " + reason)
                .build());

        // Notification
        notificationRepository.save(Notification.builder()
                .recipientUserId(currentUser.getId())
                .title("Application Rejected")
                .message("Address update request " + app.getApplicationId() + " rejected. Reason: " + reason)
                .type("ALERT")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        return getApplicationById(saved.getId());
    }

    public ApplicationDetailDTO requestInformation(Long id, String comments, UserPrincipal currentUser) {
        verifyNotAuditor(currentUser);

        if (comments == null || comments.isBlank()) {
            throw new IllegalArgumentException("Information request details cannot be empty.");
        }

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        if ("APPROVED".equalsIgnoreCase(app.getCurrentStatus()) || "REJECTED".equalsIgnoreCase(app.getCurrentStatus())) {
            throw new IllegalStateException("Cannot request information for completed/rejected application " + app.getApplicationId());
        }

        app.setCurrentStatus("INFORMATION_REQUIRED");
        app.setOfficerComments(comments);
        app.setReviewedByOfficer(currentUser.getEmployeeId() != null ? currentUser.getEmployeeId() : currentUser.getUsername());
        Application saved = applicationRepository.save(app);

        // Audit Log
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(currentUser.getId())
                .action("INFORMATION_REQUESTED")
                .result("SUCCESS")
                .description("Requested additional information for " + app.getApplicationId() + ": " + comments)
                .build());

        // Notification
        notificationRepository.save(Notification.builder()
                .recipientUserId(currentUser.getId())
                .title("Information Requested")
                .message("Additional details requested for application " + app.getApplicationId())
                .type("REQUEST")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        return getApplicationById(saved.getId());
    }

    @Transactional
    public SoapResultDTO processSoapAddressUpdate(UpdateRationAddressCommand command) {
        String corrId = command.getCorrelationId() != null ? command.getCorrelationId() : "N/A";

        // Audit Log: SOAP Request Received
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(command.getApplicationId())
                .officerId(null)
                .action("SOAP_REQUEST_RECEIVED")
                .result("SUCCESS")
                .description("UpdateRationAddress SOAP request received for App: " + command.getApplicationId() + " (CorrelationId: " + corrId + ")")
                .build());

        // Revenue Verification Check
        if (command.getRevenueVerified() == null || !command.getRevenueVerified()) {
            auditLogRepository.save(AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .applicationId(command.getApplicationId())
                    .officerId(null)
                    .action("SOAP_PROCESSING_FAILED")
                    .result("FAILED")
                    .description("SOAP update rejected: RevenueVerified must be true. (CorrelationId: " + corrId + ")")
                    .build());
            throw new SoapServiceException("VALIDATION_FAILED", "Revenue verification flag is required and must be true.");
        }

        // Application Existence Check
        Application app = applicationRepository.findByApplicationId(command.getApplicationId())
                .orElseThrow(() -> {
                    auditLogRepository.save(AuditLog.builder()
                            .timestamp(LocalDateTime.now())
                            .applicationId(command.getApplicationId())
                            .officerId(null)
                            .action("SOAP_PROCESSING_FAILED")
                            .result("FAILED")
                            .description("SOAP update rejected: Application not found: " + command.getApplicationId() + " (CorrelationId: " + corrId + ")")
                            .build());
                    return new SoapServiceException("APPLICATION_NOT_FOUND", "Application not found with Application ID: " + command.getApplicationId());
                });

        // Food Department Endpoint Idempotency Check:
        // If application is already APPROVED and address matches, return previous result without second business update!
        if ("APPROVED".equalsIgnoreCase(app.getCurrentStatus()) && command.getAddress() != null && command.getAddress().equals(app.getRequestedAddress())) {
            auditLogRepository.save(AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .applicationId(command.getApplicationId())
                    .officerId(null)
                    .action("DUPLICATE_SOAP_REQUEST_DETECTED")
                    .result("SUCCESS")
                    .description("Food SOAP endpoint detected duplicate operation for " + app.getApplicationId() + ". Returning previous successful result without duplicate database update. (CorrelationId: " + corrId + ")")
                    .build());

            return SoapResultDTO.builder()
                    .applicationId(app.getApplicationId())
                    .status("SUCCESS")
                    .message("Ration address update already processed (Food Department Idempotent Response)")
                    .correlationId(corrId)
                    .build();
        }

        // Ration Card No Mismatch Check
        if (command.getRationCardNo() == null || !app.getRationCardNo().replace("-", "").equalsIgnoreCase(command.getRationCardNo().replace("-", ""))) {
            auditLogRepository.save(AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .applicationId(command.getApplicationId())
                    .officerId(null)
                    .action("SOAP_PROCESSING_FAILED")
                    .result("FAILED")
                    .description("SOAP update rejected: Ration Card No mismatch. Expected: " + app.getRationCardNo() + ", Received: " + command.getRationCardNo())
                    .build());
            throw new SoapServiceException("VALIDATION_FAILED", "Supplied Ration Card No " + command.getRationCardNo() + " does not match application record " + app.getRationCardNo());
        }

        // Address Field Check
        if (command.getAddress() == null || command.getAddress().isBlank()) {
            throw new SoapServiceException("VALIDATION_FAILED", "Address element is required and cannot be blank.");
        }

        // Master Ration Record Lookup
        RationRecord record = rationRecordRepository.findByRationCardNo(app.getRationCardNo())
                .orElseThrow(() -> new SoapServiceException("VALIDATION_FAILED", "Linked Ration Card master record not found: " + app.getRationCardNo()));

        // Perform Transactional Update of Master Ration Record
        record.setHouseAddress(command.getAddress());
        record.setUpdateStatus("UPDATED");
        rationRecordRepository.save(record);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(null)
                .action("RATION_RECORD_UPDATED")
                .result("SUCCESS")
                .description("Updated house address for Ration Card " + record.getRationCardNo() + " to: " + command.getAddress() + " via SOAP Web Service")
                .build());

        // Perform Update of Application Record
        app.setCurrentStatus("APPROVED");
        app.setRequestedAddress(command.getAddress());
        app.setOfficerComments("Approved via GovMesh SOAP Interoperability Interface (Consent: " + command.getConsentId() + ", Correlation: " + corrId + ")");
        app.setReviewedByOfficer("SOAP_INTEROP_GATEWAY");
        applicationRepository.save(app);

        // Audit Log: SOAP Processing Success
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .applicationId(app.getApplicationId())
                .officerId(null)
                .action("SOAP_PROCESSING_SUCCESS")
                .result("SUCCESS")
                .description("Address update request " + app.getApplicationId() + " processed successfully via SOAP (CorrelationId: " + corrId + ")")
                .build());

        return SoapResultDTO.builder()
                .applicationId(app.getApplicationId())
                .status("SUCCESS")
                .message("Ration address update processed successfully")
                .correlationId(corrId)
                .build();
    }

    public List<AuditLog> getApplicationHistory(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        return auditLogRepository.filterAuditLogs(app.getApplicationId(), null, null);
    }

    private void verifyNotAuditor(UserPrincipal currentUser) {
        if ("AUDITOR".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Auditor role has read-only privileges and cannot perform application action operations.");
        }
    }

    private void verifySeniorOrAdmin(UserPrincipal currentUser) {
        if (!"SENIOR_OFFICER".equalsIgnoreCase(currentUser.getRole()) && !"DEPARTMENT_ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new UnauthorizedException("Only Senior Officers or Department Admins have authorization to approve or reject applications.");
        }
    }

    private ApplicationDTO mapToDTO(Application app) {
        return ApplicationDTO.builder()
                .id(app.getId())
                .applicationId(app.getApplicationId())
                .citizenReference(app.getCitizenReference())
                .rationCardNo(app.getRationCardNo())
                .applicationType(app.getApplicationType())
                .currentStatus(app.getCurrentStatus())
                .sourceDepartment(app.getSourceDepartment())
                .requestedAddress(app.getRequestedAddress())
                .officerComments(app.getOfficerComments())
                .reviewedByOfficer(app.getReviewedByOfficer())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private RationRecordDTO mapToRationDTO(RationRecord record) {
        return RationRecordDTO.builder()
                .id(record.getId())
                .rationCardNo(record.getRationCardNo())
                .holderName(record.getHolderName())
                .houseAddress(record.getHouseAddress())
                .talukaCode(record.getTalukaCode())
                .districtCode(record.getDistrictCode())
                .verificationFlag(record.getVerificationFlag())
                .updateStatus(record.getUpdateStatus())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
