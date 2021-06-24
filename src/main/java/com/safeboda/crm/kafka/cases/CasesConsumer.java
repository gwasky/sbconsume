package com.safeboda.crm.kafka.cases;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.safeboda.crm.utils.AgentAvailability;
import com.safeboda.crm.utils.DBUtils;
import com.safeboda.crm.utils.Utils;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class CasesConsumer {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(CasesConsumer.class.getName());

        Utils utils = new Utils();
        DBUtils dbUtils = new DBUtils();
        Properties props = utils.loadProperties();
        String bootstrapServers = null;
        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")){
            bootstrapServers = System.getenv("BROKER");
        } else{
            bootstrapServers = props.getProperty("kafka.bootstrap.server");
        }

        logger.info("BROKER - {}", bootstrapServers);
        String groupId = "backoffice-assignment-application";
        String topic = props.getProperty("kafka.backoffice.topic");

        // Create consume Configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create Consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
        consumer.subscribe(Arrays.asList(topic));

        //  poll for new data
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            String availabilityDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

            for (ConsumerRecord<String, String> record : records) {

                logger.info("Key" + record.key() + ", Value " + record.value());
                // logger.info("Partition: " + record.partition() + ", Offest " + record.offset());

                String agentAssignmentTracker = null;
                try {
                    ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
                    // logger.info(String.valueOf(scheduledAgentsAvailability));
                    boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
                    logger.info(String.valueOf(exists));
                    if (!exists) {
                        try {
                            agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
                        } catch (Exception ex) {
                            logger.error("Error Initializing Object in Redis - {}", ex.getMessage());
                        }
                    } else {
                        try {
                            agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
                        } catch (Exception ex) {
                            logger.error("Error Fetching Availability Object from Redis - {}", ex.getMessage());
                        }
                    }
                    // logger.info(agentAssignmentTracker);
                    String agentAvailabilityList = utils.updateAvailabilityTrackerWithNewlyAvailableAgents(availabilityDate,scheduledAgentsAvailability,agentAssignmentTracker);
                    if (agentAvailabilityList != null) {
                        String userId = utils.nominateUserForAssignment(agentAvailabilityList);
                        // System.out.println(userId);
                        boolean assignmentStatus = dbUtils.assignCaseToAgent(record.value(), userId);
                        logger.info("CaseID[{}] | UserID[{}] | result[{}]", record.value(), userId, assignmentStatus);
                        if (assignmentStatus) {
                            // Update Assignment Counts for the day
                            String updatedAgentTracker = utils.updateAssignmentCounts(availabilityDate, agentAvailabilityList, userId);
                            logger.info("Successful | refreshed Counts | {}", updatedAgentTracker);
                        } else {
                            logger.info("CaseID[{}] Assignment to Agent[{}] Failed", record.value(), userId);
                        }
                    } else {
                        logger.error("No Object found for Agent Assignment Tracker");
                    }
                    // System.out.println(userId);

                } catch (SQLException ex) {

                }
            }
        }

    }
}
