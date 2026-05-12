package core.domain.entity;

import java.util.Objects;

/** Player・Enemy を一意に識別する不変 ID。 */
public record ActorId(String value) {

  public ActorId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ActorId must not be blank");
    }
  }

  public static ActorId of(String value) {
    return new ActorId(value);
  }
}
