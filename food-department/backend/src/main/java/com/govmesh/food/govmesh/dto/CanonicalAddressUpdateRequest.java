package com.govmesh.food.govmesh.dto;

import java.util.List;

public class CanonicalAddressUpdateRequest {
    private String applicationId;
    private String sourceDepartment;
    private String targetDepartment;
    private String correlationId;
    private String idempotencyKey;
    private String purpose;
    private List<String> requestedFields;
    private CitizenInfo citizen;
    private VerificationInfo verification;
    private ConsentInfo consent;

    public CanonicalAddressUpdateRequest() {}

    public CanonicalAddressUpdateRequest(String applicationId, String sourceDepartment, String targetDepartment, String correlationId, CitizenInfo citizen, VerificationInfo verification, ConsentInfo consent) {
        this.applicationId = applicationId;
        this.sourceDepartment = sourceDepartment;
        this.targetDepartment = targetDepartment;
        this.correlationId = correlationId;
        this.citizen = citizen;
        this.verification = verification;
        this.consent = consent;
    }

    public CanonicalAddressUpdateRequest(String applicationId, String sourceDepartment, String targetDepartment, String correlationId, String purpose, List<String> requestedFields, CitizenInfo citizen, VerificationInfo verification, ConsentInfo consent) {
        this.applicationId = applicationId;
        this.sourceDepartment = sourceDepartment;
        this.targetDepartment = targetDepartment;
        this.correlationId = correlationId;
        this.purpose = purpose;
        this.requestedFields = requestedFields;
        this.citizen = citizen;
        this.verification = verification;
        this.consent = consent;
    }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getSourceDepartment() { return sourceDepartment; }
    public void setSourceDepartment(String sourceDepartment) { this.sourceDepartment = sourceDepartment; }

    public String getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public List<String> getRequestedFields() { return requestedFields; }
    public void setRequestedFields(List<String> requestedFields) { this.requestedFields = requestedFields; }

    public CitizenInfo getCitizen() { return citizen; }
    public void setCitizen(CitizenInfo citizen) { this.citizen = citizen; }

    public VerificationInfo getVerification() { return verification; }
    public void setVerification(VerificationInfo verification) { this.verification = verification; }

    public ConsentInfo getConsent() { return consent; }
    public void setConsent(ConsentInfo consent) { this.consent = consent; }

    public static class CitizenInfo {
        private String reference;
        private String name;
        private AddressInfo address;

        public CitizenInfo() {}
        public CitizenInfo(String reference, String name, AddressInfo address) {
            this.reference = reference;
            this.name = name;
            this.address = address;
        }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public AddressInfo getAddress() { return address; }
        public void setAddress(AddressInfo address) { this.address = address; }
    }

    public static class AddressInfo {
        private String line;
        private String district;
        private String taluka;

        public AddressInfo() {}
        public AddressInfo(String line, String district, String taluka) {
            this.line = line;
            this.district = district;
            this.taluka = taluka;
        }

        public String getLine() { return line; }
        public void setLine(String line) { this.line = line; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getTaluka() { return taluka; }
        public void setTaluka(String taluka) { this.taluka = taluka; }
    }

    public static class VerificationInfo {
        private String status;
        private String source;

        public VerificationInfo() {}
        public VerificationInfo(String status, String source) {
            this.status = status;
            this.source = source;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    public static class ConsentInfo {
        private String id;

        public ConsentInfo() {}
        public ConsentInfo(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
