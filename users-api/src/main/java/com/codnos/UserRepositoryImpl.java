package com.codnos;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import rx.Observable;
import rx.RxReactiveStreams;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;

public class UserRepositoryImpl implements UserRepository {

    private final Bucket bucket;

    public UserRepositoryImpl() {
        Cluster cluster = CouchbaseCluster.create("localhost");
        cluster.authenticate("username", "password");
        bucket = cluster.openBucket("users");
    }

    @Override
    public CompletionStage<CouchbaseUser> find(String userId) {
        CompletionStage<JsonDocument> document = fromObservable(bucket.async().get(userId));
        return document.thenApply(this::toUser);
    }

    @Override
    public Source<CouchbaseUser, NotUsed> findAll() {
        Observable<CouchbaseUser> users = bucket.async().query(select("meta(users).id", "*").from(i("users")))
                .flatMap(result ->
                        result.errors()
                                .flatMap(e -> Observable.<AsyncN1qlQueryRow>error(new CouchbaseException("N1QL Error/Warning: " + e)))
                                .switchIfEmpty(result.rows())
                )
                .map(AsyncN1qlQueryRow::value)
                .map(this::toUser);
        return Source.fromPublisher(RxReactiveStreams.toPublisher(users));
    }

    private CouchbaseUser toUser(JsonObject d) {
        JsonObject object = d.getObject("users");
        String id = d.getString("id");
        return toUser(id, object);
    }
    private CouchbaseUser toUser(JsonDocument d) {
        String id = d.id();
        JsonObject content = d.content();
        return toUser(id, content);
    }

    private CouchbaseUser toUser(String id, JsonObject content) {
        CouchbaseUser user = new CouchbaseUser();
        user.setId(id);
        user.setName(content.getString("name"));
        Integer salaryPrecision = content.getInt("salary_precision");
        user.setSalaryPrecision(salaryPrecision);
        user.setFavoriteColor(content.getString("favorite_color"));
        JsonArray structureArray = content.getArray("salary_structure");
        int size = structureArray.size();
        List<String> salaryStructure = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            salaryStructure.add(structureArray.getString(i));
        }
        user.setSalaryStructure(salaryStructure);
        JsonObject salariesObject = content.getObject("salaries");
        Map<String, List<BigDecimal>> salaries = new LinkedHashMap<>();
        for (String key : salariesObject.getNames()) {
            salaries.put(key, getDataRow(salariesObject.getString(key), salaryPrecision));
        }
        user.setSalaries(salaries);
        return user;
    }

    private List<BigDecimal> getDataRow(String binaryString, Integer salaryPrecision) {
        byte[] bytes = Base64.getDecoder().decode(binaryString);
        try {
            Users.Data data = Users.Data.parseFrom(bytes);

            List<BigDecimal> result = new ArrayList<>(data.getValuesCount());
            for (ByteString byteString : data.getValuesList()) {
                result.add(new BigDecimal(new BigInteger(byteString.toByteArray()), salaryPrecision));
            }
            return result;
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    public static <T> CompletionStage<T> fromObservable(Observable<T> observable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        observable
                .doOnError(future::completeExceptionally)
                .single()
                .forEach(future::complete);
        return future;
    }
}
