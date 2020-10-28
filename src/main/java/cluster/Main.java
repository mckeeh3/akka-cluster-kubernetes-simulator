package cluster;

import akka.actor.typed.javadsl.ActorContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;

class Main {
  static Behavior<Void> create() {
    return Behaviors.setup(context -> {
      bootstrap(context);

      return Behaviors.receive(Void.class)
        .onSignal(Terminated.class, signal -> Behaviors.stopped())
        .build();
    });
  }

  private static void bootstrap(final ActorContext<Void> context) {
    context.spawn(ClusterListenerActor.create(), "clusterListener");

    final HttpClient httpClient = startHttpClient(context.getSystem());

    context.spawn(EntityCommandActor.create(httpClient), "entityCommand");
    context.spawn(EntityQueryActor.create(httpClient), "entityQuery");
  }

  private static HttpClient startHttpClient(ActorSystem<?> actorSystem) {
    final String host = actorSystem.settings().config().getString("visualizer.http.server.host");
    final int port = actorSystem.settings().config().getInt("visualizer.http.server.port");
    return new HttpClient(actorSystem, host, port);
  }

  public static void main(String[] args) {
    final ActorSystem<?> actorSystem = ActorSystem.create(Main.create(), "cluster");
    startClusterBootstrap(actorSystem);
  }

  private static void startClusterBootstrap(ActorSystem<?> actorSystem) {
    AkkaManagement.get(actorSystem).start();
    ClusterBootstrap.get(actorSystem).start();
  }
}
