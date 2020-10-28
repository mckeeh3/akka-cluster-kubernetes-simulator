package cluster;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonCreator;
import akka.actor.typed.ActorSystem;

public interface EntityCommand extends Serializable {

  public static class ChangeValue implements EntityCommand {
    private static final long serialVersionUID = 1L;
    public final String id;
    public final Object value;
    public final long nsStart;

    @JsonCreator
    public ChangeValue(String id, Object value) {
      this.id = id;
      this.value = value;
      nsStart = System.nanoTime();
    }

    @Override
    public String toString() {
      return String.format("%s[%s, %s]", getClass().getSimpleName(), id, value);
    }
  }

  public static class ChangeValueAck implements EntityCommand {
    private static final long serialVersionUID = 1L;
    public final String id;
    public final Object value;
    public final long nsStart;
    public final String message;
    public final int httpStatusCode;

    @JsonCreator
    public ChangeValueAck(String id, Object value, long nsStart, String message, int httpStatusCode) {
      this.id = id;
      this.value = value;
      this.nsStart = nsStart;
      this.message = message;
      this.httpStatusCode = httpStatusCode;
    }

    @Override
    public String toString() {
      return String.format("%s[%,d, %s, %s, %s, %d]", getClass().getSimpleName(), nsStart, id, value, message, httpStatusCode);
    }
  }

  public static class GetValue implements EntityCommand {
    private static final long serialVersionUID = 1L;
    public final String id;
    public final long nsStart;

    @JsonCreator
    public GetValue(String id) {
      this.id = id;
      nsStart = System.nanoTime();
    }

    @Override
    public String toString() {
      return String.format("%s[%,d, %s]", getClass().getSimpleName(), nsStart, id);
    }
  }

  public static class GetValueAck implements EntityCommand {
    private static final long serialVersionUID = 1L;
    public final String id;
    public final Object value;
    public final long nsStart;
    public final String message;
    public final int httpStatusCode;

    @JsonCreator
    public GetValueAck(String id, Object value, long nsStart, String message, int httpStatusCode) {
      this.id = id;
      this.value = value;
      this.nsStart = nsStart;
      this.message = message;
      this.httpStatusCode = httpStatusCode;
    }

    @Override
    public String toString() {
      return String.format("%s[%,d, %s, %s, %s, %d]", getClass().getSimpleName(), nsStart, id, value, message, httpStatusCode);
    }
  }

  static String nodeId(ActorSystem<?> actorSystem) {
    String[] ip = actorSystem.address().getHost().orElse("err").split("\\.");
    if (ip.length < 1) {
      throw new RuntimeException(String.format("Akka host (%s) must be a valid IPv4 address."));
    }
    return ip[ip.length - 1];
  }

  static String randomEntityId(String nodeId, int range) {
    return String.format("%s-%s", nodeId, (int) Math.round(Math.random() * range));
  }
}
