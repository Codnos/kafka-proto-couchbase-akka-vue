package com.codnos;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.scaladsl.model.headers.ModeledCustomHeader;
import akka.http.scaladsl.model.headers.ModeledCustomHeaderCompanion;
import akka.stream.ActorMaterializer;
import com.google.protobuf.ByteString;
import scala.util.Success$;
import scala.util.Try;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ReadFromApi {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ActorSystem system = ActorSystem.create("read");
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        HttpRequest request = HttpRequest.create("http://localhost:8080/api/users").addHeader(new AcceptHeader("application/x-protobuf"));
        CompletionStage<String> fut = Http.get(system)
                .singleRequest(request, materializer)
                .handle((response, responseException) -> {
                    if (responseException != null) {
                        responseException.printStackTrace();
                        system.terminate();
                        return "ERROR";
                    }

                    if (response.status().isFailure()) {
                        System.out.println("Got " + response.status());
                        response.entity().toStrict(10000L, materializer).thenAccept(r ->
                                System.out.println(r.getData().utf8String()));
                        system.terminate();
                        return "HTTP_ERROR";
                    } else {
                        response.entity().toStrict(10000L, materializer).whenComplete((r, t) -> {
                                    if (t == null) {
                                        try {
                                            ByteBuffer byteBuffer = r.getData().asByteBuffer();
                                            Users.UserList userList = Users.UserList.parseFrom(ByteString.copyFrom(byteBuffer));
                                            for (Users.User user : userList.getUsersList()) {
                                                System.out.println(user);
                                                System.out.println(toMapOfBinaries(user.getSalariesMap(), user.getSalaryPrecision()));
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            system.terminate();
                                        }
                                    } else {
                                        t.printStackTrace();
                                        system.terminate();
                                    }
                                }
                        );
                        return "OK";
                    }
                });

        String result = fut.toCompletableFuture().get();
        System.out.println("run... with result: "+ result);
    }


    private static Map<String, List<BigDecimal>> toMapOfBinaries(Map<String, Users.Data> salaries, int salaryPrecision) {
        return salaries.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> getBinaries(e.getValue(), salaryPrecision)));
    }

    private static List<BigDecimal> getBinaries(Users.Data value, int salaryPrecision) {
        List<BigDecimal> result = new ArrayList<>(value.getValuesCount());
        for (ByteString byteString : value.getValuesList()) {
            BigDecimal x = new BigDecimal(new BigInteger(byteString.toByteArray()), salaryPrecision);
            result.add(x);
        }
        return result;
    }

    public static class AcceptHeader extends ModeledCustomHeader {


        private final String value;

        public AcceptHeader(String value) {
            this.value = value;
        }

        public boolean renderInResponses() {
            return false;
        }

        public boolean renderInRequests() {
            return true;
        }

        @Override
        public ModeledCustomHeaderCompanion companion() {
            return new ModeledCustomHeaderCompanion() {
                @Override
                public Try parse(String value) {
                    return Success$.MODULE$.apply(value);
                }

                @Override
                public String name() {
                    return "Accept";
                }
            };
        }

        @Override
        public String value() {
            return value;
        }
    }
}
