package com.codnos;

import com.google.protobuf.ByteString;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.util.Arrays.asList;

public class UserProducer {
    public static final int TARGET_SCALE = 15;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props);

        Map<String, List<Double>> originalSalaries = new LinkedHashMap<>();
        originalSalaries.put("Facebook", asList(45.0012d, 56.2357774d, 2345.000d));
        originalSalaries.put("Google", asList(145.0012d, 256.2357774d, 32345.000d));

        Map<String, Users.Data> transformedSalaries = new LinkedHashMap<>(originalSalaries.size());

        for (Map.Entry<String, List<Double>> salaryInfo : originalSalaries.entrySet()) {
            String company = salaryInfo.getKey();
            List<ByteString> bigSalaries = new ArrayList<>(salaryInfo.getValue().size());
            for (Double salary : salaryInfo.getValue()) {
                BigDecimal x = new BigDecimal(salary).setScale(10, RoundingMode.HALF_UP);
                BigDecimal rescaled = x.setScale(TARGET_SCALE);
                bigSalaries.add(ByteString.copyFrom(rescaled.unscaledValue().toByteArray()));
            }
            transformedSalaries.put(company, Users.Data.newBuilder().addAllValues(bigSalaries).build());
        }

        Users.User.Builder user1 = Users.User.newBuilder();
        user1.setName("Alyssa");
        user1.setFavoriteColor("blue");
        user1.addAllSalaryStructure(asList("20170101", "20170201", "20170301"));
        user1.putAllSalaries(transformedSalaries);
        user1.setSalaryPrecision(TARGET_SCALE);
        byte[] bytes = user1.build().toByteArray();
        ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>("send-user", "Alyssa", bytes);
        Future<RecordMetadata> resultFuture = producer.send(record);
        RecordMetadata recordMetadata = resultFuture.get();
        System.out.println(recordMetadata.toString());
    }
}
