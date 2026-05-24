package core.domain.tree;

import java.util.Objects;
import java.util.Set;

/**
 * ソウルツリーの 1 ノード (§15-7 / E-2)。
 *
 * <p>{@code prerequisites} は本ノードを解放するために事前に解放されている必要があるノード ID の集合。 空集合は「最初に解放可能」(通常は中央 root
 * のみ)。{@link SoulTree#unlock} で前提検証を行う。
 *
 * <p>{@code soulCost} は解放に必要なソウル量 (0 も許容、root の cost=0 用)。
 */
public record TreeNode(
    NodeId id, String displayName, int soulCost, NodeEffect effect, Set<NodeId> prerequisites) {

  public TreeNode {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (soulCost < 0) {
      throw new IllegalArgumentException("soulCost must be non-negative: " + soulCost);
    }
    Objects.requireNonNull(effect, "effect");
    Objects.requireNonNull(prerequisites, "prerequisites");
    prerequisites = Set.copyOf(prerequisites);
  }
}
