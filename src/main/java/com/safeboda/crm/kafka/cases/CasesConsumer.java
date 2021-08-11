package com.safeboda.crm.kafka.cases;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.safeboda.crm.entities.CaseAudit;
import com.safeboda.crm.utils.AgentAvailability;
import com.safeboda.crm.utils.DBUtils;
import com.safeboda.crm.utils.Utils;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

import static com.safeboda.crm.utils.Constants.*;

public class CasesConsumer {

    private static Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private static int count = 0;

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(CasesConsumer.class.getName());

        Utils utils = new Utils();
        DBUtils dbUtils = new DBUtils();
        Properties props = utils.loadProperties();

        String consumerTopic = props.getProperty("kafka.backoffice.topic");
        // String consumerRetryTopic = props.getProperty("kafka.backoffice.retry_topic");
        Properties consumerProps = utils.getConsumerProperties();
        // Create Consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerProps);
        consumer.subscribe(Arrays.asList(consumerTopic));

        try {
            //  poll for new data
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                String availabilityDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
                String availabilityDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
                for (ConsumerRecord<String, String> record : records) {

                    // logger.info("Key: " + record.key() + ",  Value: " + record.value());
                    // logger.info("Partition: " + record.partition() + ", Offest " + record.offset());

                    String agentAssignmentTracker = null;
                    try {
                        ArrayList<AgentAvailability> scheduledAgentsAvailability = dbUtils.getScheduledAgentsAndAvailability(availabilityDate);
                        logger.info("{} - Number of Scheduled Agents - {}", record.value(), scheduledAgentsAvailability.size());
                        if (scheduledAgentsAvailability.size() > 0) {
                            // logger.info(String.valueOf(scheduledAgentsAvailability));
                            boolean exists = utils.checkForObjectRedisPersistence(availabilityDate);
                            // logger.info(String.valueOf(exists));
                            if (!exists) {
                                try {
                                    agentAssignmentTracker = utils.initializeObjectInRedis(availabilityDate, scheduledAgentsAvailability);
                                } catch (Exception ex) {
                                    logger.error("{} - Error Initializing Object in Redis - {}", record.value(), ex.getMessage());
                                    ex.printStackTrace();
                                    utils.produceRecord(props.getProperty("kafka.backoffice.retry_topic"), record.value());
                                }
                            } else {
                                try {
                                    agentAssignmentTracker = utils.getAvailabilityObjectFromRedis(availabilityDate);
                                } catch (Exception ex) {
                                    logger.error("{} - Error Fetching Availability Object from Redis - {}", record.value(), ex.getMessage());
                                    ex.printStackTrace();
                                    utils.produceRecord(props.getProperty("kafka.backoffice.retry_topic"), record.value());
                                }
                            }
                            // logger.info(agentAssignmentTracker);
                            String agentAvailabilityList = utils.updateAvailabilityTrackerWithNewlyAvailableAgents(availabilityDate, scheduledAgentsAvailability, agentAssignmentTracker);
                            logger.info("{} - agentAvailabilityList - {}", record.value(), agentAvailabilityList);
                            if (!agentAvailabilityList.equals("[]")) {
                                String userId = utils.nominateUserForAssignment(agentAvailabilityList);
                                // System.out.println(userId);
                                // Log the Ticket to Audit Audit Table
                                String caseStatus = dbUtils.getCaseStatus(record.value());
                                CaseAudit caseAudit = new CaseAudit(record.value(), userId, caseStatus,SOURCE);
                                int result = dbUtils.logToAuditTable(caseAudit);
                                logger.info("Log to Audit Table {}|{}|{}|{}", record.value(), userId, caseStatus,result);
                                // Assign to Agent
                                String response = utils.logon();
                                Map<String, String> responseMap = utils.jsonStringToMap(response);
                                logger.info(responseMap.toString());
                                String jsonObject = utils.buildCaseJSONObject
                                        (responseMap.get("id"), record.value(), userId).toString();
                                int statusCode = utils.updateCase(jsonObject);
                                // boolean assignmentStatus = dbUtils.assignCaseToAgent(record.value(), userId);
                                logger.info("CaseID[{}] | UserID[{}] | StatusCode[{}]", record.value(), userId, statusCode);
                                if (statusCode == 200) {
                                    // Update Assignment Counts for the day
                                    String updatedAgentTracker = utils.updateAssignmentCounts(availabilityDate, agentAvailabilityList, userId);
                                    logger.info("Successful | refreshed Counts | {}", updatedAgentTracker);
//                                    try {
//                                        currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1, "no metadata"));
//                                        if (count % 2 == 0)
//                                            consumer.commitAsync(currentOffsets, null);
//                                        count++;
//                                    } catch (CommitFailedException e) {
//                                        e.printStackTrace();
//                                    }

                                } else {
                                    logger.info("CaseID[{}] Assignment to Agent[{}] Failed", record.value(), userId);
                                    // Send to Retry Topic
                                    utils.produceRecord(props.getProperty("kafka.backoffice.topic"), record.value());
                                }
                            } else {
                                logger.error("{} - No Object found for Agent Assignment Tracker", record.value());
                                utils.produceRecord(props.getProperty("kafka.backoffice.retry_topic"), record.value());
                            }
                        } else {
                            logger.info("There are no Available Agents at - {} - {}", availabilityDateTime, record.value());
                            TimeUnit.MINUTES.sleep(1);
                            utils.produceRecord(props.getProperty("kafka.backoffice.topic"), record.value());
                        }
                    } catch (SQLException ex) {
                        logger.error("getScheduledAgentsAndAvailability - Exception - {}", ex.getMessage());
                        utils.produceRecord(props.getProperty("kafka.backoffice.retry_topic"), record.value());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        utils.produceRecord(props.getProperty("kafka.backoffice.retry_topic"), record.value());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Closing will Clean up Sockets in use, alert the coordinator about the consumer's departure from a group
            consumer.close();
        }
    }
}
