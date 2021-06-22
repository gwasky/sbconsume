package com.safeboda.crm.utils;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.safeboda.crm.kafka.cases.CasesConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

public class DBUtils {

    private String availabilityDate;
    Logger logger = LoggerFactory.getLogger(DBUtils.class.getName());
    private String configFileName = "config.properties";
    private String query = "select \n" +
            "\tscheduler.schedule_from_date,\n" +
            "\tscheduler.schedule_to_date,\n" +
            "\tscheduled_agents.name agent_name,\n" +
            "\tscheduled_agents.user_id_c,\n" +
            "\tagent_availability.name availability_date,\n" +
            "\tagent_availability.date_entered,\n" +
            "\tagent_availability.available\n" +
            "from bo_bo_scheduler scheduler\n" +
            "left outer join bo_bo_scheduler_bo_bo_scheduled_agents_c scheduled_agents_j on scheduler.id = scheduled_agents_j.bo_bo_scheduler_bo_bo_scheduled_agentsbo_bo_scheduler_ida\n" +
            "left outer join bo_bo_scheduled_agents scheduled_agents on scheduled_agents.id = scheduled_agents_j.bo_bo_scheduler_bo_bo_scheduled_agentsbo_bo_scheduled_agents_idb \n" +
            "left outer join bo_bo_scheduled_agents_bo_bo_agent_availability_c agent_availability_j on agent_availability_j.bo_bo_scheadd3_agents_ida = scheduled_agents.id\n" +
            "left outer join bo_bo_agent_availability agent_availability on agent_availability_j.bo_bo_schee90fability_idb = agent_availability.id\n" +
            "where agent_availability.name = ? ";

    private String assignCaseQuery = "update cases set assigned_user_id = ? where id = ?";

    public DBUtils(String availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public Connection getDBConnection(){
        Utils utils = new Utils();
        Connection conn = null;
        try {
            Properties properties = utils.loadProperties();
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUser(properties.getProperty("db.user"));
            dataSource.setPassword(properties.getProperty("db.password"));
            dataSource.setServerName(properties.getProperty("db.server"));
            dataSource.setPortNumber(Integer.parseInt(properties.getProperty("db.port")));
            dataSource.setDatabaseName(properties.getProperty("db.name"));
            conn = dataSource.getConnection();
        } catch (Exception ex){
            logger.error(ex.getMessage());
        }finally {

        }
        return conn;
    }


    public ArrayList<AgentAvailability> getScheduledAgentsAndAvailability(String availabilityDate) throws SQLException {
        ArrayList<AgentAvailability> agents = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            conn = getDBConnection();
            preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1,availabilityDate);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                String scheduleFromDate = resultSet.getString("schedule_from_date");
                String scheduleToDate = resultSet.getString("schedule_to_date");
                String agentName = resultSet.getString("agent_name");
                String agentID = resultSet.getString("user_id_c");
                String availabitityDate = resultSet.getString("availability_date");
                String dateEntered = resultSet.getString("date_entered");
                String availabile = resultSet.getString("available");
                AgentAvailability agentAvailability = new AgentAvailability(scheduleFromDate,scheduleToDate,agentName,agentID,availabitityDate,dateEntered,availabile);
                // logger.info(String.valueOf(agentAvailability));
                agents.add(agentAvailability);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }finally {
            if (preparedStatement != null){
                try{
                    preparedStatement.close();
                }catch (Exception ex){
                    logger.error(ex.getMessage());
                }
            }
            if (conn != null){
                conn.close();
            }
            if (resultSet != null){
                resultSet.close();
            }
        }
        return agents;
    }

    public boolean assignCaseToAgent(String caseId,String userId){
        Connection conn = null;
        try {
            conn = getDBConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(assignCaseQuery);
            preparedStatement.setString(1,userId);
            preparedStatement.setString(2,caseId);
            return preparedStatement.execute();
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
        return false;
    }






}
