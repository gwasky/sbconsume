package com.safeboda.crm.entities;

/**
 * @author Gibson Wasukira
 * @created 22/08/2021 - 11:07 AM
 */
public class QueueAudit {

    private String boQueueId;
    private String caseId;

    public QueueAudit() {
    }

    public QueueAudit(String boQueueId, String caseId) {
        this.boQueueId = boQueueId;
        this.caseId = caseId;
    }

    public String getBoQueueId() {
        return boQueueId;
    }

    public void setBoQueueId(String boQueueId) {
        this.boQueueId = boQueueId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    @Override
    public String toString() {
        return "QueueAudit{" +
                "boQueueId='" + boQueueId + '\'' +
                ", caseId='" + caseId + '\'' +
                '}';
    }
}
