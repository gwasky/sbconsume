package com.safeboda.crm.kafka.cases;


import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Properties;

public class CasesConsumer {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(CasesConsumer.class.getName());

        String bootstrapServers = "127.0.0.1:29092";
        String groupId = "my-application";
        String topic = "test";

        // Create consume Configs
        Properties properties =  new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        // Create Consumer
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);
        consumer.subscribe(Arrays.asList(topic));

        //  poll for new data
        while (true){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
            for (ConsumerRecord<String,String> record : records){
                logger.info("Key" + record.key() + ", Value " + record.value());
                logger.info("Partition: "+ record.partition() + ", Offest "+ record.offset());

            }
        }

    }
}
