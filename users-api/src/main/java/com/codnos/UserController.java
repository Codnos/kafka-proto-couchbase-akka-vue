package com.codnos;


import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.MediaType;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Rejections;
import akka.http.javadsl.server.Route;
import akka.http.scaladsl.common.EntityStreamingSupport;
import akka.http.scaladsl.model.ContentType$;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.javadsl.Sink;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import com.google.protobuf.ByteString;
import scala.collection.JavaConverters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static akka.http.javadsl.server.PathMatchers.segment;
import static java.util.Arrays.asList;

public class UserController extends AllDirectives {

    private final UserRepository userRepository;
    private final ActorMaterializer materializer;

    public UserController(UserRepository userRepository, ActorMaterializer materializer) {
        this.userRepository = userRepository;
        this.materializer = materializer;
    }

    public Route createRoute() {
        akka.http.scaladsl.model.MediaType.Compressibility comp = akka.http.scaladsl.model.MediaType.NotCompressible$.MODULE$;
        akka.http.scaladsl.model.MediaType.Binary proto = akka.http.scaladsl.model.MediaType.applicationBinary("x-protobuf", comp, JavaConverters.asScalaBuffer(Collections.<String>emptyList()));
        akka.http.scaladsl.model.MediaType.WithOpenCharset json = akka.http.scaladsl.model.MediaType.applicationWithOpenCharset("json", JavaConverters.asScalaBuffer(Collections.<String>emptyList()));
        ContentType protoContent = ContentType$.MODULE$.apply(proto);
        return route(
                get(() ->
                        pathPrefix("api", () ->
                                path(segment("user").slash(segment()), (String userId) ->
                                        completeOKWithFuture(userRepository.find(userId), Jackson.marshaller())
                                ).orElse(
                                        path("users", () ->
                                                extract(r -> r.getRequest().getHeaders(), (headers) -> {
                                                            java.util.Optional<HttpHeader> accept = StreamSupport.stream(headers.spliterator(), false).filter(h -> h.is("accept"))
                                                                    .findFirst();
                                                            String acceptedListAsString = accept.map(HttpHeader::value).orElse("");
                                                            List<String> accepted = Arrays.stream(acceptedListAsString.split(",")).map(String::trim).collect(Collectors.toList());
                                                            if (accepted.contains("application/json")) {
                                                                return completeOKWithSource(userRepository.findAll(), Jackson.marshaller(), EntityStreamingSupport.json());
                                                            } else if (accepted.contains("application/x-protobuf")) {
                                                                SerializeAllToProtobuf x = new SerializeAllToProtobuf();
                                                                CompletionStage<byte[]> result = userRepository.findAll().via(x).runWith(Sink.head(), materializer);
                                                                Marshaller<byte[], RequestEntity> marshaller = Marshaller.withFixedContentType(protoContent, bytes -> HttpEntities.create(protoContent, bytes));
                                                                return completeOKWithFuture(result, marshaller);
                                                            } else {
                                                                Iterable<MediaType> supported = asList(proto, json);
                                                                return reject(Rejections.unsupportedRequestContentType(supported));
                                                            }
                                                        }
                                                )
                                        )
                                )
                        )
                ));
    }


    public class SerializeAllToProtobuf extends GraphStage<FlowShape<CouchbaseUser, byte[]>> {

        public final Inlet<CouchbaseUser> in = Inlet.create("SerializeAllToProtobuf.in");
        public final Outlet<byte[]> out = Outlet.create("SerializeAllToProtobuf.out");

        private final FlowShape<CouchbaseUser, byte[]> shape = FlowShape.of(in, out);

        @Override
        public GraphStageLogic createLogic(Attributes inheritedAttributes) throws Exception {
            return new GraphStageLogic(shape) {
                Users.UserList.Builder userList = Users.UserList.newBuilder();

                {
                    setHandler(
                            in,
                            new AbstractInHandler() {
                                @Override
                                public void onPush() {
                                    CouchbaseUser elem = grab(in);
                                    Users.User user = convert(elem);
                                    userList.addUsers(user);
                                }

                                @Override
                                public void onUpstreamFinish() {
                                    emit(out, userList.build().toByteArray());
                                    complete(out);
                                }
                            });

                    setHandler(
                            out,
                            new AbstractOutHandler() {
                                @Override
                                public void onPull() throws Exception {
                                    pull(in);
                                }
                            });
                }
            };
        }

        private Users.User convert(CouchbaseUser elem) {
            Users.User.Builder builder = Users.User.newBuilder().setName(elem.getName())
                    .setSalaryPrecision(elem.getSalaryPrecision())
                    .addAllSalaryStructure(elem.getSalaryStructure())
                    .putAllSalaries(convertSalaries(elem.getSalaries()));
            if (elem.getFavoriteColor() != null) {
                builder.setFavoriteColor(elem.getFavoriteColor());
            }
            return builder.build();
        }

        private Map<String, Users.Data> convertSalaries(Map<String, List<BigDecimal>> originalSalaries) {
            Map<String, Users.Data> transformedSalaries = new LinkedHashMap<>(originalSalaries.size());

            for (Map.Entry<String, List<BigDecimal>> salaryInfo : originalSalaries.entrySet()) {
                String company = salaryInfo.getKey();
                List<ByteString> bigSalaries = new ArrayList<>(salaryInfo.getValue().size());
                for (BigDecimal salary : salaryInfo.getValue()) {
                    bigSalaries.add(ByteString.copyFrom(salary.unscaledValue().toByteArray()));
                }
                transformedSalaries.put(company, Users.Data.newBuilder().addAllValues(bigSalaries).build());
            }
            return transformedSalaries;
        }

        @Override
        public FlowShape<CouchbaseUser, byte[]> shape() {
            return shape;
        }
    }

//    @GetMapping("/api/users-without-salaries")
//    public Flux<MongoUser> getAllUsersWithoutSalaries() {
//        return userRepository.findAllExcludingSalaries();
//    }
//
}
