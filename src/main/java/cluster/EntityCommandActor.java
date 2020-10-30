package cluster;

import java.time.Duration;
import java.util.Date;
import org.slf4j.Logger;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

class EntityCommandActor extends AbstractBehavior<EntityCommand> {
  private final ActorContext<EntityCommand> actorContext;
  private final HttpClient httpClient;
  private final int entitiesPerNode;
  private final IpId.Client client;

  static Behavior<EntityCommand> create(HttpClient httpClient) {
    return Behaviors.setup(actorContext -> 
        Behaviors.withTimers(timer -> new EntityCommandActor(actorContext, timer, httpClient)));
  }

  private EntityCommandActor(ActorContext<EntityCommand> actorContext, TimerScheduler<EntityCommand> timerScheduler, HttpClient httpClient) {
    super(actorContext);
    this.actorContext = actorContext;
    this.httpClient = httpClient;

    entitiesPerNode = actorContext.getSystem().settings().config().getInt("entity-actor.entities-per-node");
    client = IpId.Client.of(actorContext.getSystem());

    final Duration interval = Duration.parse(actorContext.getSystem().settings().config().getString("entity-actor.command-tick-interval-iso-8601"));
    timerScheduler.startTimerWithFixedDelay(Tick.ticktock, interval);
  }

  @Override
  public Receive<EntityCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(Tick.class, t -> onTick())
        .onMessage(EntityCommand.ChangeValueAck.class, this::onChangeValueAck)
        .build();
  }

  private Behavior<EntityCommand> onTick() {
    final String entityId = EntityCommand.randomEntityId(client.id, entitiesPerNode);
    final EntityCommand.ChangeValue changeValue = new EntityCommand.ChangeValue(entityId, new Date(), System.nanoTime(), client);
    log().info("Request {}", changeValue);
    actorContext.pipeToSelf(
      httpClient.post(changeValue),
      (r, e) -> {
        if (e == null) {
          return r;
        } else {
          return new EntityCommand.ChangeValueAck(changeValue.id, changeValue.value, changeValue.nsStart, e.getMessage(), 500);
        }
      });
    return this;
  }

  private Behavior<EntityCommand> onChangeValueAck(EntityCommand.ChangeValueAck changeValueAck) {
    log().info("Response {}", changeValueAck);
    return this;
  }

  private Logger log() {
    return actorContext.getSystem().log();
  }

  enum Tick implements EntityCommand {
    ticktock
  }
}
