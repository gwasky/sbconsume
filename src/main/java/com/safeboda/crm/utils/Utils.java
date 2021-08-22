package com.safeboda.crm.utils;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    Logger logger = LoggerFactory.getLogger(Utils.class.getName());
    private String configFileName = "config.local.properties";

    public Utils() {
    }


    public Properties getProducerProperties() {
        Properties props = loadProperties();
        String bootstrapServers = null;

        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
        } else if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("dev")){
            configFileName = "config.dev.docker.properties";
        }

        bootstrapServers = props.getProperty("kafka.bootstrap.server");

        logger.info("BROKER - {}", bootstrapServers);

        // Create consume Configs
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    public void produceRecord(String topic, String message) {
        try {
            logger.info("Recycling Message - {} - to {}", message, topic);
            Properties properties = getProducerProperties();
            // Create the Producer
            KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
            // Create a producer Record
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
            // producer.send(record);
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    // runs whenever a record is successfully produced or throws an exception if it failed
                    if (e == null) {
                        // if exception is null,
                        logger.info("Received new Metadata, Offset {}", recordMetadata.offset());
                    } else {
                        logger.error("Error Producing - {}", message);
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Error Producing");
        }
        // Wait until data is produced, this makes the producer synchronous
        // producer.flush();
        // producer.close();
    }

    public Properties getConsumerProperties() {
        Properties props = loadProperties();
        String bootstrapServers = null;

        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
        } else if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("dev")){
            configFileName = "config.dev.docker.properties";
        }

        bootstrapServers = props.getProperty("kafka.bootstrap.server");

        logger.info("BROKER - {}", bootstrapServers);

        String groupId = "backoffice-assignment-application";

        // Create consume Configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // properties.put("enable.auto.commit", "false");
        return properties;
    }

    public Properties loadProperties() {

        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
        } else if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("dev")){
            configFileName = "config.dev.docker.properties";
        }
        // Load Properties file from classpath
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFileName)) {
            if (inputStream == null) {
                logger.info("Unable to find configuration file " + configFileName);
                return properties;
            }
            properties.load(inputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    public Jedis redisClient() {
        Properties properties = loadProperties();
        try {
            // logger.info("Connecting to Redis {} - {}", properties.getProperty("redis.server"), Integer.parseInt(properties.getProperty("redis.port")));
            Jedis jedis = new Jedis(properties.getProperty("redis.server"), Integer.parseInt(properties.getProperty("redis.port")));
            // System.out.println(properties.getProperty("redis.password"));
            if (jedis != null) {
                jedis.auth(properties.getProperty("redis.password"));
            }
            // logger.info("Connection to Redis Successful");
            return jedis;
        } catch (JedisConnectionException ex) {
            // logger.error(ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            // logger.error(ex.getMessage());
        }
        return null;
    }

    public boolean checkForObjectRedisPersistence(String agentAvailabilityDate) {
        boolean exists = false;
        try {
            Jedis redisClient = redisClient();
            if (redisClient.exists(agentAvailabilityDate)) {
                // String value = redisClient.get(agentAvailabilityDate);
                // System.out.println(value);
                exists = true;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return exists;
    }

    public String initializeObjectInRedis(String availabilityDate, ArrayList<AgentAvailability> agents) {
        ArrayList<AgentAssignmentTracker> agentTrackerList = new ArrayList<>();
        for (AgentAvailability agent : agents) {
            AgentAssignmentTracker agentAssignmentTracker = new AgentAssignmentTracker(agent.getAgentID(), agent.getAvailabile(), 0);
            agentTrackerList.add(agentAssignmentTracker);
            //System.out.println(agent.getAgentID());
        }
        // Store
        Jedis redisClient = redisClient();
        Gson gson = new Gson();
        String json = gson.toJson(agentTrackerList);
        redisClient.set(availabilityDate, json);
        return json;
    }

    public String getAvailabilityObjectFromRedis(String key) {
        String availabilityObj = null;
        try {
            Jedis redisClient = redisClient();
            availabilityObj = redisClient.get(key);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return availabilityObj;
    }

    public String nominateUserForAssignment(String agents) {
        String userId = null;
        Gson gson = new Gson();
        ArrayList<AgentAssignmentTracker> agentsList = new ArrayList<>();
        // Deserialize
        JsonArray arr = new JsonParser().parse(agents).getAsJsonArray();
        for (JsonElement jsonElement : arr) {
            agentsList.add(gson.fromJson(jsonElement, AgentAssignmentTracker.class));
        }
        logger.info(String.valueOf(agentsList));
        List<AgentAssignmentTracker> availableAgents = agentsList.stream()
                .filter(p -> p.getAgentAvailability().endsWith("yes")).collect(Collectors.toList());
        if (availableAgents.size() > 0) {
            AgentAssignmentTracker availableAgentWithLeastAssignments = availableAgents.stream().min(Comparator.comparing(AgentAssignmentTracker::getCount)).orElseThrow(NoSuchElementException::new);
            if (availableAgentWithLeastAssignments != null) {
                userId = availableAgentWithLeastAssignments.getAgentId();
                return userId;
            }
        } else {
            logger.info("There are no available agents");
        }
        return userId;
    }

    public String updateAssignmentCounts(String availabilityDate, String agents, String userId) {
        JsonArray arr = new JsonParser().parse(agents).getAsJsonArray();
        ArrayList<AgentAssignmentTracker> agentsList = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement jsonElement : arr) {
            agentsList.add(gson.fromJson(jsonElement, AgentAssignmentTracker.class));
        }
        agentsList.stream().forEach(p -> {
            if (p.getAgentId().equals(userId)) {
                p.setCount(p.getCount() + 1);
            }
        });
        Jedis redisClient = redisClient();
        String json = gson.toJson(agentsList);
        redisClient.set(availabilityDate, json);
        return json;
    }

    public String updateAvailabilityTrackerWithNewlyAvailableAgents(String availabilityDate, ArrayList<AgentAvailability> scheduledAgentsAvailability, String agents) {

        ArrayList<AgentAssignmentTracker> agentsList = new ArrayList<>();
        Gson gson = new Gson();
        if (agents != null) {
            JsonArray arr = new JsonParser().parse(agents).getAsJsonArray();
            for (JsonElement jsonElement : arr) {
                agentsList.add(gson.fromJson(jsonElement, AgentAssignmentTracker.class));
            }
        }
        // Generate list of newly available agents
        List<String> availableAgents = scheduledAgentsAvailability.stream()
                .map(AgentAvailability::getAgentID)
                .collect(Collectors.toList());

//        logger.info("availableAgents - {}",availableAgents);

        List<String> cachedAvailableAgents = agentsList.stream()
                .map(AgentAssignmentTracker::getAgentId)
                .collect(Collectors.toList());

//        logger.info("currentCachedList - {}",agentsList);
//        logger.info("cachedAvailableAgents - {}",cachedAvailableAgents);

        // Update Cache Object with Availability Status
        agentsList.stream().forEach(p -> {
            if (availableAgents.contains(p.getAgentId())) p.setAgentAvailability("yes");
            else p.setAgentAvailability("no");
        });

//        logger.info("Updated Cached Agent List [ with Availability Status ] - {}",cachedAvailableAgents);

        // Add newly Available Agents, [new - old] = new available
        List<String> newAvailableAgents = availableAgents.stream()
                .filter(e -> !cachedAvailableAgents.contains(e))
                .collect(Collectors.toList());

//        logger.info("newAvailableAgents - {}",newAvailableAgents);

        scheduledAgentsAvailability.stream().forEach(i -> {
            if (newAvailableAgents.contains(i.getAgentID())) {
                agentsList.add(new AgentAssignmentTracker(i.getAgentID(), "yes", 0));
            }
        });

        // logger.info("agentsList - {}",agentsList);

        Jedis redisClient = redisClient();
        String json = gson.toJson(agentsList);

        if (json != null) {
            redisClient.set(availabilityDate, json);
        }
        return json;
    }

    public void sendSMS(String message) throws Exception {
        Utils utils = new Utils();
        Properties props = utils.loadProperties();
        String[] phoneNumberList = new String[1000];
        String phoneNumbers = props.getProperty("phone_numbers");
        if (phoneNumbers != null) {
            phoneNumberList = phoneNumbers.split(",");
        }
        HttpPost post = new HttpPost("http://caresmsgroup.com/api.php");

        for (String phoneNumber : phoneNumberList) {
            // add request parameter, form parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("user", "Ricky2015"));
            urlParameters.add(new BasicNameValuePair("password", "123456"));
            urlParameters.add(new BasicNameValuePair("sender", "New-world"));
            urlParameters.add(new BasicNameValuePair("message", message));
            urlParameters.add(new BasicNameValuePair("reciever", phoneNumber));
            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(post)) {
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        }

    }

    public String logon() throws Exception {
        String resp;
        Properties props = loadProperties();
        HttpPost httpPost = new HttpPost(props.getProperty("crm.rest.api.url"));
        String rest_data = buildJSONObject(props).toString();
        System.out.println(rest_data);
        // add request parameter, form parameters
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("method", "login"));
        urlParameters.add(new BasicNameValuePair("input_type", "JSON"));
        urlParameters.add(new BasicNameValuePair("response_type", "JSON"));
        urlParameters.add(new BasicNameValuePair("rest_data", rest_data));
        httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
        System.out.println(httpPost.toString());
        // System.exit(0);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            resp = EntityUtils.toString(response.getEntity());
        }
        return resp;
    }

    public JSONObject buildJSONObject(Properties props) {
        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            Field changeMap = jsonObject.getClass().getDeclaredField("map");
            changeMap.setAccessible(true);
            changeMap.set(jsonObject, new LinkedHashMap<>());
            changeMap.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.info(e.getMessage());
        }
        jsonObject.put("user_name", props.getProperty("crm.username"));
        jsonObject.put("password", props.getProperty("crm.password"));
        jsonObject.put("version", props.getProperty("crm.version"));
        jsonObject1.put("user_auth", jsonObject);
        return jsonObject1;
    }

    public JSONObject buildCaseJSONObject(String sessionId, String caseId, String assignedUserId) {

        JSONObject jsonObject = new JSONObject();
        try {
            Field changeMap = jsonObject.getClass().getDeclaredField("map");
            changeMap.setAccessible(true);
            changeMap.set(jsonObject, new LinkedHashMap<>());
            changeMap.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.info(e.getMessage());
        }

        jsonObject.put("session", sessionId);
        jsonObject.put("module_name", "Cases");

        JSONObject jsonObjectNameValueList = new JSONObject();
        try {
            Field changeMap = jsonObjectNameValueList.getClass().getDeclaredField("map");
            changeMap.setAccessible(true);
            changeMap.set(jsonObjectNameValueList, new LinkedHashMap<>());
            changeMap.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.info(e.getMessage());
        }
        jsonObjectNameValueList.put("id", caseId);
        jsonObjectNameValueList.put("assigned_user_id", assignedUserId);
        jsonObjectNameValueList.put("status", "Open_Assigned");

        jsonObject.put("name_value_list", jsonObjectNameValueList);

        return jsonObject;
    }

    public Map<String, String> jsonStringToMap(String jsonString) {
        Map<String, String> map = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(jsonString, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


    public int updateCase(String rest_data) throws Exception {
        int resp;
        Properties props = loadProperties();
        HttpPost httpPost = new HttpPost(props.getProperty("crm.rest.api.url"));
        // String rest_data = buildJSONObject(props).toString();
        System.out.println(rest_data);
        // add request parameter, form parameters
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("method", "set_entry"));
        urlParameters.add(new BasicNameValuePair("input_type", "JSON"));
        urlParameters.add(new BasicNameValuePair("response_type", "JSON"));
        urlParameters.add(new BasicNameValuePair("rest_data", rest_data));
        httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
        System.out.println(httpPost.toString());
        // System.exit(0);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            // resp = EntityUtils.toString(response.getEntity());
            logger.info("{}|{}", response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
            resp = response.getStatusLine().getStatusCode();
        }
        return resp;
    }


}
