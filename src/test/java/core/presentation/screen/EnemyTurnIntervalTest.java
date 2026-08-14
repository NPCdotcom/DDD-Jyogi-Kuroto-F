package core.presentation.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnemyTurnIntervalTest {

  @Test
  void intervalForZeroApIsFast() {
    float interval = DungeonScreen.calculateEnemyStepInterval(0);
    assertEquals(0.05f, interval, 1e-4f, "AP 0 の場合は即座に消化する 0.05s");
  }

  @Test
  void intervalForSmallApIsCappedAtMax() {
    float interval = DungeonScreen.calculateEnemyStepInterval(2);
    assertEquals(0.10f, interval, 1e-4f, "AP が少ない場合は上限の 0.10s");
  }

  @Test
  void intervalForLargeApIsProportionalToTargetTime() {
    float interval = DungeonScreen.calculateEnemyStepInterval(20);
    // 1.2 / 20 = 0.06s
    assertEquals(0.06f, interval, 1e-4f, "総 AP 20 の場合は 1.2s / 20 = 0.06s");
  }

  @Test
  void intervalForExtremeApIsFlooredAtMin() {
    float interval = DungeonScreen.calculateEnemyStepInterval(100);
    assertEquals(0.03f, interval, 1e-4f, "極端に AP が多い場合は下限 0.03s");
  }

  @Test
  void simulateRenderLoop60FpsCompletesWithinTargetTime() {
    int totalAp = 28;
    float interval = DungeonScreen.calculateEnemyStepInterval(totalAp);
    float delta = 1.0f / 60.0f; // 60fps
    float timer = 0f;
    float totalTime = 0f;
    int actionsDone = 0;

    while (actionsDone < totalAp) {
      totalTime += delta;
      timer += delta;
      while (timer >= interval && actionsDone < totalAp) {
        timer -= interval;
        actionsDone++;
      }
    }
    // 28 * 0.043s ≈ 1.2s + 1 frame Margin
    assertTrue(totalTime <= 1.35f, "28 AP でも 60fps シミュレーションで 1.35 秒以内に完了すること: " + totalTime);
  }

  @Test
  void simulateRenderLoop144FpsCompletesWithinTargetTime() {
    int totalAp = 28;
    float interval = DungeonScreen.calculateEnemyStepInterval(totalAp);
    float delta = 1.0f / 144.0f; // 144fps
    float timer = 0f;
    float totalTime = 0f;
    int actionsDone = 0;

    while (actionsDone < totalAp) {
      totalTime += delta;
      timer += delta;
      while (timer >= interval && actionsDone < totalAp) {
        timer -= interval;
        actionsDone++;
      }
    }
    assertTrue(totalTime <= 1.35f, "28 AP でも 144fps シミュレーションで 1.35 秒以内に完了すること: " + totalTime);
  }
}
