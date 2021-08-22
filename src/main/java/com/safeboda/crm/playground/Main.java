package com.safeboda.crm.playground;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.safeboda.crm.entities.CaseAudit;
import com.safeboda.crm.entities.QueueAudit;
import com.safeboda.crm.utils.AgentAvailability;
import com.safeboda.crm.utils.DBUtils;
import com.safeboda.crm.utils.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        DBUtils dbUtils = new DBUtils();
        Utils utils = new Utils();
//        try {
//            utils.sendSMS("Test Message");
//            System.exit(0);
//            ArrayList<String> phoneNumber = new ArrayList<>();
//            String agentAssignmentTracker = null;
//            String availabilityDate = "2021-06-24";
//            ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
//            boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
//            // logger.info(String.valueOf(exists));
//            if (!exists) {
//                agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
//            } else {
//                // Update
//                agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
//            }
//            String agentAvailabilityList = utils.updateAvailabilityTrackerWithNewlyAvailableAgents(availabilityDate,scheduledAgentsAvailability,agentAssignmentTracker);
//            logger.info(agentAvailabilityList);
//            // System.exit(0);
//            if (!agentAvailabilityList.equals("[]")) {
//                // Deserialize
//                String userId = utils.nominateUserForAssignment(agentAvailabilityList);
//                if (true) {
//                    // Update Assignment Counts for the day
//                    utils.updateAssignmentCounts(availabilityDate, agentAvailabilityList, userId);
//                    logger.info("Successful");
//                } else {
//                    logger.info("Failed");
//                }
//                // System.out.println(userId);
//            } else {
//                logger.info("No Agents Assigned");
//            }
//        } catch (SQLException ex) {
//            logger.error(ex.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//
//        String caseStatus = dbUtils.getCaseStatus("ab096278-1ca1-f9f7-d3d1-60f04b32eedb");
//        System.out.println(caseStatus);

//        CaseAudit caseAudit = new CaseAudit("kajlsdlakdlajdlada", "hkalsdlaldkalda", "Closed_Closed");
//        int result = dbUtils.logToAuditTable(caseAudit);
//        System.out.println(result);


//        String jsonString = new JSONObject()
//                .put("JSON1", "Hello World!")
//                .put("JSON2", "Hello my World!")
//                .put("JSON3", new JSONObject().put("key1", "value1"))
//                .toString();
//
//        String tt = utils.logon();
//        Map<String, String> map = utils.jsonStringToMap(tt);
//        System.out.println(map);
//
//        String json2String = utils.buildCaseJSONObject
//                (map.get("id"), "2755314a-1111-ab5a-b4a2-611360b21b0a", "13e11a82-b55d-6465-a564-5e1ebd9fc2b1").toString();
//
//        String json2String = new JSONObject()
//                .put("session", map.get("id"))
//                .put("module_name", "Cases")
//                .put("name_value_list", new JSONObject()
//                        .put("id", "2755314a-1111-ab5a-b4a2-611360b21b0a")
//                        .put("assigned_user_id", "b44c3327-f243-b37f-34cd-60ec344e1131")
//                )
//                .toString();
//
//        System.out.println(json2String);
//
//        int statusCode = utils.updateCase(json2String);
//
//        if (statusCode == 200) {
//            System.out.println("Success");
//        }
//        System.out.println(statusCode);

//        String tt = utils.logon();
//        Map <String,String> map = utils.jsonStringToMap(tt);
//        System.out.println(map);

//        String tt = printJSONObject().toString();
//        System.out.println(tt);

//            String json2String1 = new JSONObject()
//                .put("session", map.get("id"))
//                .put("module_name", "Cases")
//                .put("name_value_list", new JSONObject()
//                        .put("id", "2755314a-1111-ab5a-b4a2-611360b21b0a")
//                        .put("assigned_user_id", "b44c3327-f243-b37f-34cd-60ec344e1131")
//                )
//                .toString();

//
//            JSONObject jsonObject = new JSONObject();
//            try {
//                Field changeMap = jsonObject.getClass().getDeclaredField("map");
//                changeMap.setAccessible(true);
//                changeMap.set(jsonObject, new LinkedHashMap<>());
//                changeMap.setAccessible(false);
//            } catch (IllegalAccessException | NoSuchFieldException e) {
//                logger.info(e.getMessage());
//            }
//
//            jsonObject.put("session", "session");
//            jsonObject.put("module_name", "Cases");
//
//            JSONObject jsonObjectNameValueList = new JSONObject();
//            try {
//                Field changeMap = jsonObjectNameValueList.getClass().getDeclaredField("map");
//                changeMap.setAccessible(true);
//                changeMap.set(jsonObjectNameValueList, new LinkedHashMap<>());
//                changeMap.setAccessible(false);
//            } catch (IllegalAccessException | NoSuchFieldException e) {
//                logger.info(e.getMessage());
//            }
//            jsonObjectNameValueList.put("id", "id");
//            jsonObjectNameValueList.put("assigned_user_id", "assigned_user_id");
//
//            jsonObject.put("name_value_list", jsonObjectNameValueList);
//
//            System.out.println(jsonObject.toString());


    String str = "{\"boQueueId\":\"24b8bc5a-428e-9b66-6324-5e4b7c976dfe\",\"caseId\":\"82ed4f8d-8f08-9b0a-28e8-612205774b92\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        QueueAudit queueAudit = objectMapper.readValue(str, QueueAudit.class);
        System.out.println(queueAudit);

    }


}
