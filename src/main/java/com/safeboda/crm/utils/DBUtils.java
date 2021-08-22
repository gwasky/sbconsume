package com.safeboda.crm.utils;

/**
 * @author Gibson Wasukira
 * @created 23/06/2021 - 6:59 PM
 */

import com.mysql.cj.jdbc.MysqlDataSource;
import com.safeboda.crm.entities.CaseAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

import static com.safeboda.crm.utils.Constants.*;

public class DBUtils {

    private String availabilityDate;
    Logger logger = LoggerFactory.getLogger(DBUtils.class.getName());
    //private static final Logger logger = Logger.getLogger("BUNDLE-SERVICE");
    private String configFileName = "config.local.properties";


    public DBUtils() {
    }


    public Connection getDBConnection() {
        Utils utils = new Utils();
        Connection conn = null;
        // logger.info("Env - {}", System.getenv("OP_ENV"));
        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
        } else if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("dev")){
            configFileName = "config.dev.docker.properties";
        }
        try {
            Properties properties = utils.loadProperties();
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUser(properties.getProperty("db.crm.user"));
            dataSource.setPassword(properties.getProperty("db.crm.password"));
            dataSource.setServerName(properties.getProperty("db.crm.server"));
            dataSource.setPortNumber(Integer.parseInt(properties.getProperty("db.crm.port")));
            dataSource.setDatabaseName(properties.getProperty("db.crm.name"));
            conn = dataSource.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return conn;
    }

    public Connection getPostgresDBConnection() {
        Utils utils = new Utils();
        Connection conn = null;
        // logger.info("Env - {}", System.getenv("OP_ENV"));
        if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("production")) {
            configFileName = "config.properties";
        } else if (System.getenv("OP_ENV") != null && System.getenv("OP_ENV").equals("dev")){
            configFileName = "config.dev.docker.properties";
        }
        try {
            Properties properties = utils.loadProperties();
            Properties props = new Properties();
            props.setProperty("user", properties.getProperty("db.dwh.user"));
            props.setProperty("password", properties.getProperty("db.dwh.password"));
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(properties.getProperty("db.dwh.url"), props);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return conn;
    }


    public ArrayList<AgentAvailability> getScheduledAgentsAndAvailability(String availabilityDate,String deptName) throws SQLException {
        ArrayList<AgentAvailability> agents = new ArrayList<>();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            conn = getDBConnection();
            preparedStatement = conn.prepareStatement(QUERY_GET_AVAILABLE_AGENTS);
            preparedStatement.setString(1, availabilityDate);
            preparedStatement.setString(2, deptName);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String scheduleFromDate = resultSet.getString("schedule_from_date");
                String scheduleToDate = resultSet.getString("schedule_to_date");
                String agentName = resultSet.getString("agent_name");
                String agentID = resultSet.getString("user_id_c");
                String availabitityDate = resultSet.getString("availability_date");
                String dateEntered = resultSet.getString("date_entered");
                String availabile = resultSet.getString("available");
                String department = resultSet.getString("department");
                AgentAvailability agentAvailability = new AgentAvailability(scheduleFromDate, scheduleToDate, agentName, agentID, availabitityDate, dateEntered, availabile,department);
                // logger.info(String.valueOf(agentAvailability));
                agents.add(agentAvailability);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // logger.log(Level.INFO, "{0}", new Object[]{e});
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
            if (conn != null) {
                conn.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return agents;
    }

    public boolean assignCaseToAgent(String caseId, String userId) {
        Connection conn;
        boolean status = false;
        try {
            conn = getDBConnection();
            String assignCaseQuery = "update cases set assigned_user_id = ? where id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(assignCaseQuery);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, caseId);
            int x = preparedStatement.executeUpdate();
            // System.out.println(x);
            if (x == 1) status = true;
            conn.close();
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return status;
    }

    public String getCaseStatus(String caseId) {
        Connection conn;
        String status = null;
        try {
            conn = getDBConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(QUERY_GET_CASE_STATUS);
            preparedStatement.setString(1, caseId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                status = rs.getString("status");
            }
            rs.close();
            preparedStatement.close();
            conn.close();
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return status;
    }

    public int logToAuditTable(CaseAudit caseAudit) {
        Connection conn = null;
        int status = 0;
        try {
            conn = getPostgresDBConnection();
            // Create table if it doesnt exist
            PreparedStatement ps = conn.prepareStatement(QUERY_CREATE_CASE_AUDIT_TABLE);
            int ddl = ps.executeUpdate();
            ps.close();
            if (ddl == 1) logger.info("Created Case Audit Table - {}", ddl);
            PreparedStatement preparedStatement = conn.prepareStatement(QUERY_LOG_TO_CASE_AUDIT);
            preparedStatement.setString(1, caseAudit.getUserId());
            preparedStatement.setString(2, caseAudit.getCaseId());
            preparedStatement.setString(3, caseAudit.getCaseStatus());
            preparedStatement.setString(4, caseAudit.getSource());
            status = preparedStatement.executeUpdate();
            logger.info("Logging Assigned Case to Audit | {} {} {} | {}", caseAudit.getUserId(), caseAudit.getCaseId(), caseAudit.getCaseStatus(), status);
            preparedStatement.close();
            conn.close();
        } catch (SQLException ex) {
            logger.error("SQL State : {} {}", ex.getSQLState(), ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return status;
    }


}
