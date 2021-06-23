package com.safeboda.crm.kafka.cases;


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
        String bootstrapServers = props.getProperty("kafka.bootstrap.server");
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
                        agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
                    } else {
                        agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
                    }
                    logger.info(agentAssignmentTracker);
                    if ( agentAssignmentTracker != null) {
                        String userId = utils.nominateUserForAssignment(agentAssignmentTracker);
                        // System.out.println(userId);
                        boolean assignmentStatus = dbUtils.assignCaseToAgent(record.value(),userId);
                        logger.info("CaseID[{}] | UserID[{}] | result[{}]", record.value(),userId,assignmentStatus);
                        if (assignmentStatus) {
                            // Update Assignment Counts for the day
                            String updatedAgentTracker = utils.updateAssignmentCounts(availabilityDate, agentAssignmentTracker, userId);
                            logger.info("Successful | refreshed Counts | {}", updatedAgentTracker);
                        } else {
                            logger.info("Failed");
                        }
                    }else{
                        logger.error("No Object found for Agent Assignment Tracker");
                    }
                    // System.out.println(userId);

                } catch (SQLException ex) {

                }

            }
        }

    }
}
