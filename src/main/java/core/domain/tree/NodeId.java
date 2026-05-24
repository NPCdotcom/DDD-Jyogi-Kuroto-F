package core.domain.tree;

import java.util.Objects;

/**
 * ソウルツリーのノード識別子 (§15-7 / E-2)。値オブジェクト。
 *
 * <p>{@link SoulTree} の {@code unlockedNodes: Set<NodeId>} と {@link TreeNode#prerequisites()} で
 * 構造的等価による参照に使う (record の等価性で {@code Set.contains} が機能する)。
 */
public record NodeId(String value) {
  public NodeId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }

  public static NodeId of(String value) {
    return new NodeId(value);
  }
}
