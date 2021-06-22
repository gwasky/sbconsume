package com.safeboda.crm.playground;

import com.safeboda.crm.utils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Properties;

public class Main {

    static Logger logger = LoggerFactory.getLogger(DBUtils.class.getName());

    public static void main(String[] args) {

        DBUtils dbUtils = new DBUtils("");
        try {
            dbUtils.getScheduledAgentsAndAvailability("2021-06-29");
        } catch (SQLException ex){
            logger.error(ex.getMessage());
        }


//        Properties properties = dbUtils.getDBCredential();
//        System.out.println(properties.getProperty("db.user"));
//        System.out.println(properties.getProperty("db.password"));
    }
}
