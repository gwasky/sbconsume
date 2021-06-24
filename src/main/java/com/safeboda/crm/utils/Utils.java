package com.safeboda.crm.utils;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    Logger logger = LoggerFactory.getLogger(Utils.class.getName());
    private String configFileName = "config.local.properties";

    public Utils() {
    }

    public Properties loadProperties() {

        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
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
            logger.info("Connecting to Redis {} - {}", properties.getProperty("redis.server"), Integer.parseInt(properties.getProperty("redis.port")));
            Jedis jedis = new Jedis(properties.getProperty("redis.server"), Integer.parseInt(properties.getProperty("redis.port")));
            // System.out.println(properties.getProperty("redis.password"));
            if (jedis != null) {
                jedis.auth(properties.getProperty("redis.password"));
            }
            logger.info("Connection to Redis Successful");
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
}
