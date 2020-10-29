package cluster;

import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.headers.RawHeader;
import akka.stream.Materializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class HttpClient {
  private final ActorSystem<?> actorSystem;
  private final Materializer materializer;
  private final String url;

  public HttpClient(ActorSystem<?> actorSystem, String host, int port) {
    this.actorSystem = actorSystem;
    this.materializer = Materializer.matFromSystem(actorSystem.classicSystem());
    url = String.format("http://%s:%d/", host, port);
    actorSystem.log().info("Start HTTP Clinet {}-{}", host, port);
  }

  HttpClient(ActorSystem<?> actorSystem, String url) {
    this.actorSystem = actorSystem;
    this.materializer = Materializer.matFromSystem(actorSystem.classicSystem());
    this.url = url;
  }

  CompletionStage<EntityCommand.ChangeValueAck> post(EntityCommand.ChangeValue changeValue) {
    return Http.get(actorSystem.classicSystem())
        .singleRequest(HttpRequest.POST(url + "entity-change")
            .withHeaders(Collections.singletonList(RawHeader.create("Connection", "close")))
            .withEntity(toHttpEntity(changeValue)))
        .thenCompose(r -> {
            if (r.status().isSuccess()) {
              return Jackson.unmarshaller(EntityCommand.ChangeValueAck.class).unmarshal(r.entity(), materializer);
            } else {
              return CompletableFuture.completedFuture(
                new EntityCommand.ChangeValueAck(changeValue.id, changeValue.value, changeValue.nsStart, r.status().reason(), r.status().intValue()));
            }
        });
  }

  CompletionStage<EntityCommand.GetValueAck> post(EntityCommand.GetValue getValue) {
    return Http.get(actorSystem.classicSystem())
        .singleRequest(HttpRequest.POST(url + "entity-query")
            .withHeaders(Collections.singletonList(RawHeader.create("Connection", "close")))
            .withEntity(toHttpEntity(getValue)))
        .thenCompose(r -> {
            if (r.status().isSuccess()) {
              return Jackson.unmarshaller(EntityCommand.GetValueAck.class).unmarshal(r.entity(), materializer);
            } else {
              return CompletableFuture.completedFuture(
                new EntityCommand.GetValueAck(getValue.id, null, getValue.nsStart, r.status().reason(), r.status().intValue()));
            }
        });
  }

  private static HttpEntity.Strict toHttpEntity(Object pojo) {
    return HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(pojo).getBytes());
  }

  private static String toJson(Object pojo) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    try {
      return ow.writeValueAsString(pojo);
    } catch (JsonProcessingException e) {
      return String.format("{ \"error\" : \"%s\" }", e.getMessage());
    }
  }

  static class Id implements CborSerializable {
    final String id;

    @JsonCreator
    Id(String id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return String.format("%s[%s]", getClass().getSimpleName(), id);
    }
  }

  static class Value implements CborSerializable {
    final Object value;

    @JsonCreator
    Value(Object value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("%s[%s]", getClass().getSimpleName(), value);
    }
  }
}
