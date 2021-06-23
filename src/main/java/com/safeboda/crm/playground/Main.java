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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) {

        DBUtils dbUtils = new DBUtils("");
        Utils utils = new Utils();
        try {
            // AgentAssignmentTracker agentAssignmentTracker = null;
            String agentAssignmentTracker = null;
            // String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
            // System.out.println(timeStamp);
            // System.exit(0);
            String availabilityDate = "2021-06-22";
            ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
            logger.info(String.valueOf(scheduledAgentsAvailability));
            boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
            // logger.info(String.valueOf(exists));
            if (!exists) {
                agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
            } else {
                // Update
                agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
            }

            if (agentAssignmentTracker != null) {
                // Deserialize
                String userId = utils.nominateUserForAssignment(agentAssignmentTracker);
                // System.out.println(userId);
                // boolean assignmentStatus = dbUtils.assignCaseToAgent("",userId);
                if (true) {
                    // Update Assignment Counts for the day
                    utils.updateAssignmentCounts(availabilityDate, agentAssignmentTracker, userId);
                    logger.info("Successful");
                } else {
                    logger.info("Failed");
                }
                // System.out.println(userId);
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        }
    }


}
