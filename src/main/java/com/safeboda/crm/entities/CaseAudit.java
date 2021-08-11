package com.safeboda.crm.entities;

/**
 * @author Gibson Wasukira
 * @created 10/08/2021 - 11:59 AM
 */
public class CaseAudit {

    private String caseId;
    private String userId;
    private String caseStatus;
    private String source;

    public CaseAudit(String caseId, String userId, String caseStatus, String source) {
        this.caseId = caseId;
        this.userId = userId;
        this.caseStatus = caseStatus;
        this.source = source;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getSource() {
        return source;
    }

    public String getUserId() {
        return userId;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }


    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "CaseAudit{" +
                "caseId='" + caseId + '\'' +
                ", userId='" + userId + '\'' +
                ", caseStatus='" + caseStatus + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
