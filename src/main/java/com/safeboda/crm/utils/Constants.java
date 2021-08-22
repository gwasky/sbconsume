package com.safeboda.crm.utils;

import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * @author Gibson Wasukira
 * @created 10/08/2021 - 11:05 AM
 */
public final class Constants {

    private Constants() {
    }

    public static final String SOURCE = "CaseAssignmentConsumer";

    public static final String QUERY_CREATE_CASE_AUDIT_TABLE = "CREATE TABLE IF NOT EXISTS staging.back_office_case_audit\n" +
            "(\n" +
            "  id SERIAL PRIMARY KEY,\n" +
            "  user_id varchar,\n" +
            "  case_id varchar,\n" +
            "  status varchar,\n" +
            "  activity_date timestamp without time zone default (now() at time zone 'utc'),\n" +
            "  source varchar\n" +
            ");";

    public static final String QUERY_LOG_TO_CASE_AUDIT = "INSERT INTO staging.back_office_case_audit (user_id, case_id, status,source) VALUES (?,?,?,?)";

    public static final String QUERY_GET_CASE_STATUS = "select status from cases where id = ?";

    public static final  String QUERY_GET_AVAILABLE_AGENTS = "select \n" +
            "\tscheduler.schedule_from_date,\n" +
            "\tscheduler.schedule_to_date,\n" +
            "\tscheduled_agents.name agent_name,\n" +
            "\tscheduled_agents.user_id_c,\n" +
            "\tagent_availability.name availability_date,\n" +
            "\tagent_availability.date_entered,\n" +
            "\tagent_availability.available,\n" +
            "\tusers.department\n" +
            "from bo_bo_scheduler scheduler\n" +
            "left outer join bo_bo_scheduler_bo_bo_scheduled_agents_c scheduled_agents_j on scheduler.id = scheduled_agents_j.bo_bo_scheduler_bo_bo_scheduled_agentsbo_bo_scheduler_ida\n" +
            "left outer join bo_bo_scheduled_agents scheduled_agents on scheduled_agents.id = scheduled_agents_j.bo_bo_scheduler_bo_bo_scheduled_agentsbo_bo_scheduled_agents_idb \n" +
            "left outer join bo_bo_scheduled_agents_bo_bo_agent_availability_c agent_availability_j on agent_availability_j.bo_bo_scheadd3_agents_ida = scheduled_agents.id\n" +
            "left outer join bo_bo_agent_availability agent_availability on agent_availability_j.bo_bo_schee90fability_idb = agent_availability.id\n" +
            "left outer join bo_bo_schedule_slots slots on slots.id = scheduled_agents.bo_bo_schedule_slots_id_c\n" +
            "left outer join users users on scheduled_agents.user_id_c = users .id\n" +
            "where agent_availability.name = ? and agent_availability.available = 'yes' and users.department = ?\n" +
            "\tand convert(replace(substring(time(date_sub(UTC_TIMESTAMP(), interval -3 hour)),1,5),':',''),unsigned integer) >= convert(substring_index(slots.name,'|',1),unsigned integer)\n" +
            "\tand convert(replace(substring(time(date_sub(UTC_TIMESTAMP(), interval -3 hour)),1,5),':',''),unsigned integer) < convert(substring_index(slots.name,'|',-1),unsigned integer)";

}
