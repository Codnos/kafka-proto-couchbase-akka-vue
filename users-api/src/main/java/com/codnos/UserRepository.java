package com.codnos;

import akka.NotUsed;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

public interface UserRepository {

    CompletionStage<CouchbaseUser> find(String userId);

    Source<CouchbaseUser, NotUsed> findAll();
}