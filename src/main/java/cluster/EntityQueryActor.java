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
  private final IpId.Client client;

  static Behavior<EntityCommand> create(HttpClient httpClient) {
    return Behaviors.setup(actorContext -> 
        Behaviors.withTimers(timer -> new EntityQueryActor(actorContext, timer, httpClient)));
  }

  private EntityQueryActor(ActorContext<EntityCommand> actorContext, TimerScheduler<EntityCommand> timerScheduler, HttpClient httpClient) {
    super(actorContext);
    this.actorContext = actorContext;
    this.httpClient = httpClient;

    entitiesPerNode = actorContext.getSystem().settings().config().getInt("entity-actor.entities-per-node");
    client = IpId.Client.of(actorContext.getSystem());

    final var interval = Duration.parse(actorContext.getSystem().settings().config().getString("entity-actor.query-tick-interval-iso-8601"));
    timerScheduler.startTimerWithFixedDelay(Tick.ticktock, interval);
  }

  @Override
  public Receive<EntityCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(Tick.class, t -> onTick())
        .onMessage(EntityCommand.GetValueAck.class, this::onGetValueAck)
        .build();
  }

  private Behavior<EntityCommand> onTick() {
    final var entityId = EntityCommand.randomEntityId(client.id, entitiesPerNode);
    final var getValue = new EntityCommand.GetValue(entityId, System.nanoTime(), client);
    log().info("Request {}", getValue);
    actorContext.pipeToSelf(
      httpClient.post(getValue),
      (r, e) -> {
        if (e == null) {
          return r;
        } else {
          return new EntityCommand.GetValueAck(getValue.id, null, getValue.nsStart, e.getMessage(), 500);
        }
      });
    return this;
  }

  private Behavior<EntityCommand> onGetValueAck(EntityCommand.GetValueAck getValueAck) {
    log().info("Response {}", getValueAck);
    return this;
  }
    
  private Logger log() {
    return actorContext.getSystem().log();
  }

  enum Tick implements EntityCommand {
    ticktock
  }
}
