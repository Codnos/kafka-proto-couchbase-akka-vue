package com.codnos;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserConsumer {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        String groupId = "couchbase-storage";
        String inputTopic = "send-user";
        String brokers = "localhost:9092";
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(createConsumerConfig(brokers, groupId));
        Cluster cluster = CouchbaseCluster.create("localhost");
        cluster.authenticate("username", "password");
        Bucket bucket = cluster.openBucket("users");

        consumer.subscribe(Collections.singletonList(inputTopic));
        System.out.println("Reading topic:" + inputTopic);

        while (true) {
            ConsumerRecords<String, byte[]> records = consumer.poll(1000);
            for (ConsumerRecord<String, byte[]> record : records) {
                byte[] value = record.value();
                Users.User user = Users.User.parseFrom(value);
                JsonObject document = JsonObject.create();
                document.put("name", user.getName());
                document.put("favorite_color", user.getFavoriteColor());
                document.put("salary_precision", user.getSalaryPrecision());
                document.put("salary_structure", JsonArray.from(user.getSalaryStructureList()));
                document.put("salaries", JsonObject.from(toMapOfBinaries(user.getSalariesMap())));
                JsonDocument jsonDocument = JsonDocument.create(UUID.randomUUID().toString(), document);
                System.out.println("Inserting " + jsonDocument);
                bucket.insert(jsonDocument);
            }
        }
    }

    private static Map<String, String> toMapOfBinaries(Map<String, Users.Data> salaries) {
        return salaries.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> getBinaries(e.getValue())));
    }

    private static String getBinaries(Users.Data value) {
        return Base64.getEncoder().encodeToString(value.toByteArray());
    }

    private static Properties createConsumerConfig(String brokers, String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("group.id", groupId);
        props.put("auto.commit.enable", "true");
        props.put("auto.offset.reset", "latest");
        props.put("specific.avro.reader", true);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());

        return props;
    }

}
