package com.safeboda.crm.playground;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.safeboda.crm.utils.AgentAvailability;
import com.safeboda.crm.utils.DBUtils;
import com.safeboda.crm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) {

        DBUtils dbUtils = new DBUtils("");
        Utils utils = new Utils();
        try {
            ArrayList<String> phoneNumber = new ArrayList<>();
//            phoneNumber.add("256759333961");
//            utils.sendSMS(
//                    new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime()) + " | There are Currently No Back-Office Agents Available for Case Escalation",
//                    phoneNumber);
//            System.exit(0);
            String agentAssignmentTracker = null;
            // String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
            // System.out.println(timeStamp);
            // System.exit(0);
            String availabilityDate = "2021-06-24";
            ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
            // logger.info(String.valueOf(scheduledAgentsAvailability));
            // System.exit(0);
            boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
            // logger.info(String.valueOf(exists));
            if (!exists) {
                agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
            } else {
                // Update
                agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);


            }
            String agentAvailabilityList = utils.updateAvailabilityTrackerWithNewlyAvailableAgents(availabilityDate,scheduledAgentsAvailability,agentAssignmentTracker);
            logger.info(agentAvailabilityList);
            // System.exit(0);
            if (!agentAvailabilityList.equals("[]")) {
                // Deserialize
                String userId = utils.nominateUserForAssignment(agentAvailabilityList);
                // System.out.println(userId);
                // boolean assignmentStatus = dbUtils.assignCaseToAgent("",userId);
                if (true) {
                    // Update Assignment Counts for the day
                    utils.updateAssignmentCounts(availabilityDate, agentAvailabilityList, userId);
                    logger.info("Successful");
                } else {
                    logger.info("Failed");
                }
                // System.out.println(userId);
            } else {
                logger.info("No Agents Assigned");
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
