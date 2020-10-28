package cluster;

import java.time.Duration;
import org.slf4j.Logger;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import cluster.EntityCommand.GetValue;

class EntityQueryActor extends AbstractBehavior<EntityCommand> {
  private final ActorContext<EntityCommand> actorContext;
  private final HttpClient httpClient;
  private final int entitiesPerNode;
  private final String nodeId;

  static Behavior<EntityCommand> create(HttpClient httpClient) {
    return Behaviors.setup(actorContext -> 
        Behaviors.withTimers(timer -> new EntityQueryActor(actorContext, timer, httpClient)));
  }

  private EntityQueryActor(ActorContext<EntityCommand> actorContext, TimerScheduler<EntityCommand> timerScheduler, HttpClient httpClient) {
    super(actorContext);
    this.actorContext = actorContext;
    this.httpClient = httpClient;

    entitiesPerNode = actorContext.getSystem().settings().config().getInt("entity-actor.entities-per-node");
    nodeId = EntityCommand.nodeId(actorContext.getSystem());

    final Duration interval = Duration.parse(actorContext.getSystem().settings().config().getString("entity-actor.query-tick-interval-iso-8601"));
    timerScheduler.startTimerWithFixedDelay(Tick.ticktock, interval);
  }

  @Override
  public Receive<EntityCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(Tick.class, t -> onTick())
        .build();
  }

  private Behavior<EntityCommand> onTick() {
    final String entityId = EntityCommand.randomEntityId(nodeId, entitiesPerNode);
    final GetValue getValue = new EntityCommand.GetValue(entityId, System.nanoTime());
    log().info("Request {}", getValue);
    httpClient.post(getValue)
      .thenAccept(t -> {
        if (t.httpStatusCode != 200) {
          log().warn("Get value request failed {}", t);
        } else {
          log().info("Response {}", t);
        }
      });
    return this;
  }
    
  private Logger log() {
    return actorContext.getSystem().log();
  }

  enum Tick implements EntityCommand {
    ticktock
  }
}
