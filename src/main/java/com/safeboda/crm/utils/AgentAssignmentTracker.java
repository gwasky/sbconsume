package com.safeboda.crm.utils;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

public class AgentAssignmentTracker {

    private String agentId;
    private String agentAvailability;
    private int count;

    public AgentAssignmentTracker(String agentId, String agentAvailability, int count) {
        this.agentId = agentId;
        this.agentAvailability = agentAvailability;
        this.count = count;
    }

    public AgentAssignmentTracker() {
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentAvailability() {
        return agentAvailability;
    }

    public void setAgentAvailability(String agentAvailability) {
        this.agentAvailability = agentAvailability;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "AgentAssignmentTracker{" +
                "agentId='" + agentId + '\'' +
                ", agentAvailability='" + agentAvailability + '\'' +
                ", count=" + count +
                '}';
    }
}
