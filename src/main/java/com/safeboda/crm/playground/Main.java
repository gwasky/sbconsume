package com.safeboda.crm.playground;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.safeboda.crm.utils.AgentAssignmentTracker;
import com.safeboda.crm.utils.AgentAvailability;
import com.safeboda.crm.utils.DBUtils;
import com.safeboda.crm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) {

        DBUtils dbUtils = new DBUtils("");
        Utils utils = new Utils();
        try {
            // AgentAssignmentTracker agentAssignmentTracker = null;
            String agentAssignmentTracker = null;
            String availabilityDate = "2021-06-29";
            ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
            boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
            // logger.info(String.valueOf(exists));
            if (!exists){
                agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate,scheduledAgentsAvailability);
            } else {
                // Update
                agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
                // Deserialize
                String userId = utils.nominateUserForAssignment(agentAssignmentTracker);
                // System.out.println(userId);
                // boolean assignmentStatus = dbUtils.assignCaseToAgent("",userId);
                if (true) {
                    // Update Assignment Counts for the day
                    utils.updateAssignmentAccounts(availabilityDate,agentAssignmentTracker,userId);
                    logger.info("Successful");
                } else {
                    logger.info("Failed");
                }
                // System.out.println(userId);
            }
        } catch (SQLException ex){
            logger.error(ex.getMessage());
        }
    }


}
