package core.presentation.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import core.domain.battle.BattleAction;
import core.domain.common.Direction;
import java.util.Optional;

/**
 * キーボード入力を {@link BattleAction} にマッピングする。
 *
 * <p>1 フレームで複数キーが押されても先勝ち。Application 層から poll される。
 */
public final class PlayerInputs {

  private PlayerInputs() {}

  public static Optional<BattleAction> poll() {
    if (isJustPressed(Keys.UP, Keys.W)) {
      return Optional.of(new BattleAction.Move(Direction.UP));
    }
    if (isJustPressed(Keys.DOWN, Keys.S)) {
      return Optional.of(new BattleAction.Move(Direction.DOWN));
    }
    if (isJustPressed(Keys.LEFT, Keys.A)) {
      return Optional.of(new BattleAction.Move(Direction.LEFT));
    }
    if (isJustPressed(Keys.RIGHT, Keys.D)) {
      return Optional.of(new BattleAction.Move(Direction.RIGHT));
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
      return Optional.of(new BattleAction.UseSkill(0));
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
      return Optional.of(new BattleAction.UseSkill(1));
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
      return Optional.of(new BattleAction.UseSkill(2));
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
      return Optional.of(new BattleAction.UseSkill(3));
    }
    if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
      return Optional.of(new BattleAction.Wait());
    }
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      return Optional.of(new BattleAction.EndTurn());
    }
    return Optional.empty();
  }

  private static boolean isJustPressed(int... keys) {
    for (int k : keys) {
      if (Gdx.input.isKeyJustPressed(k)) {
        return true;
      }
    }
    return false;
  }
}
