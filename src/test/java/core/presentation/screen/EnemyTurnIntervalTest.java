package core.presentation.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
