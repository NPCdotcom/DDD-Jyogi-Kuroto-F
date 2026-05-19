package core.presentation.render;

/**
 * 画面テキストの文言定数。日本語と英語の 2 セットを持つ。
 *
 * <p>{@link Fonts#isJapaneseAvailable()} の真偽でどちらのセットを使うかを呼び出し側が判断する。 国際化ライブラリ (gdx-i18n /
 * .properties) は MVP では過剰なので、定数クラスで足りる範囲にする (YAGNI)。文言が増えてきたら {@link
 * com.badlogic.gdx.utils.I18NBundle} に切り替える。
 */
public final class Strings {

  private Strings() {}

  /** 日本語版。Noto Sans JP が assets/fonts に置かれているときに使う。 */
  public static final class Ja {
    private Ja() {}

    public static final String TITLE = "DDD-Jyogi-Kuroto-F";
    public static final String SUBTITLE = "- どこでも動くローグ -";
    public static final String START_HINT = "ENTER で出発する";
    public static final String CONTROLS_HEADER = "操作";
    public static final String CONTROLS_MOVE = "WASD / 矢印キー   1 マス移動 (AP 1)";
    public static final String CONTROLS_SKILL = "1 〜 9         カード選択 → 方向キーで使用";
    public static final String CONTROLS_WAIT = "SPACE          待機 (AP 1)";
    public static final String CONTROLS_END = "ENTER          ターン終了";

    public static final String HUD_HP = "HP";
    public static final String HUD_AP = "AP";
    public static final String HUD_SOUL = "ソウル";
    public static final String HUD_PHASE = "フェーズ";
    public static final String HUD_SPEED = "速度";
    public static final String HUD_HINT = "WASD/矢印: 移動   1〜9: カード   SPACE: 待機   ENTER: ターン終了";

    // 移動権保持中の操作ヒント (ADR-21 §15-5)
    public static final String MOVE_TOKEN_HINT = "移動権保持中: WASD/矢印で移動";

    public static final String PHASE_PLAYER = "あなたのターン";
    public static final String PHASE_ENEMY = "敵のターン";
    public static final String PHASE_GAMEOVER = "ゲームオーバー";
    public static final String PHASE_CLEARED = "踏破成功";

    public static final String GAME_OVER_HEADER = "敗 北";
    public static final String CLEARED_HEADER = "フロア踏破";
    public static final String SOULS_KEPT = "持ち帰ったソウル: ";
    public static final String NEW_RUN_HINT = "ENTER で新たな挑戦を始める";

    // CLEARED 状態 (階段踏破直後) の操作ヒント (§15-6 / §15-8 / E-6)。
    // ポップアップが前面に出るが、HUD ヒント領域にも同等情報を残して二重視認性を確保。
    public static final String CLEARED_HINT = "層末ノード選択中: 1〜3 で選択";

    // 層末ノード選択ポップアップのタイトル (§15-8 / E-6)
    public static final String LAYER_END_TITLE = "層末ノード — 1 つ選んでください";

    // 手札表示ラベル
    public static final String HAND_LABEL = "手札:";
    public static final String HAND_HINT = "数字キー: カード選択  方向キー: 使用方向  ESC: キャンセル";
    public static final String CARD_ELEMENT_PHYSICAL = "物";
    public static final String CARD_ELEMENT_MAGICAL = "魔";

    // 移動権ラベル (ADR-21 §15-5: 移動カードで付与された無料移動権の残量表示)
    public static final String MOVE_TOKEN_REMAINING_FORMAT = "移動権 残 %d 歩";

    // BattleEvent の説明文 (HUD ログ用)
    public static final String EV_MOVED_FORMAT = "%s が (%d, %d) へ移動";
    public static final String EV_SKILL_USED_FORMAT = "%s が「%s」を発動";
    public static final String EV_DAMAGE_FORMAT = "%s → %s に %d ダメージ (残 HP %d)";
    public static final String EV_DIED_FORMAT = "%s 撃破";
    public static final String EV_SOUL_GAINED_FORMAT = "%s は %d ソウルを獲得";
    public static final String EV_PHASE_FORMAT = "フェーズ移行 → %s";
    public static final String EV_REJECTED_FORMAT = "%s: 拒否 (%s)";
    public static final String EV_MOVEMENT_GRANTED_FORMAT = "%s に移動権 %d 歩を付与";
    public static final String EV_TRAP_PLACED_FORMAT = "%s が (%d, %d) に罠を設置";
    public static final String EV_TRAP_TRIGGERED_FORMAT = "%s が罠を踏み %d ダメージ (残 HP %d)";
    public static final String EV_FLOOR_ADVANCED_FORMAT = "%d 層に到達";
    /** Buff 適用ログ: %s = ActorId、%s = BuffKind 表示名、%+d = 符号付き量、%d = 残ターン数。 */
    public static final String EV_BUFF_APPLIED_FORMAT = "%s に %s %+d (残 %d ターン)";

    /** BuffKind の日本語表示名。 */
    public static final String BUFF_KIND_PHYSICAL_ATTACK = "物攻";
    public static final String BUFF_KIND_MAGICAL_ATTACK = "魔攻";
    public static final String BUFF_KIND_PHYSICAL_DEFENSE = "物防";
    public static final String BUFF_KIND_MAGICAL_DEFENSE = "魔防";
    public static final String BUFF_KIND_SPEED = "速度";

    // §15-2 / §15-9 金貨 (ラン内通貨)
    public static final String HUD_GOLD = "金貨";
    public static final String EV_GOLD_GAINED_FORMAT = "%s は %d 金貨を獲得";

    // §15-3 / §15-6 強化個体撃破通知
    public static final String EV_ELITE_DEFEATED_FORMAT = "%s を撃破! カード追加チャンス";

    // §15-7 / E-2 ソウルツリー画面 / タイトル画面動線
    public static final String SOUL_COST_FORMAT = "ソウル %d";
    public static final String SOUL_TREE_TITLE = "ソウルツリー";
    public static final String SOUL_TREE_INVENTORY_FORMAT = "所持ソウル: %d";
    public static final String SOUL_TREE_CONTROLS_HINT =
        "[クリック] ノード解放   [R] 全リセット (累計返却)   [ESC] タイトルへ戻る";
    public static final String SOUL_TREE_FLASH_RESET = "ツリーをリセットしました";
    public static final String TITLE_OPEN_TREE_HINT_FORMAT = "[T] ソウルツリーを開く (所持ソウル: %d)";

    // §15-10 / E-10 チュートリアル
    public static final String TUTORIAL_TITLE = "操作説明";
    public static final String TUTORIAL_BODY =
        """
        WASD / 矢印キー : 1 マス移動 (1 AP 消費)
        1 - 9 : 手札のカードを選択
        矢印キー (選択中) : カード使用方向を指定
        SPACE : 1 ターン待機
        ENTER : ターン終了

        階段 (黄) に到達すると次層へ進める。
        敵を倒すとソウル + 金貨を獲得。
        層末でステ強化 / ショップ / イベント のノードを選択。""";
    public static final String TUTORIAL_CLOSE_HINT = "ENTER または ESC で閉じる";
  }

  /** 英語版 (フォント未配置時の fallback)。 */
  public static final class En {
    private En() {}

    public static final String TITLE = "DDD-Jyogi-Kuroto-F";
    public static final String SUBTITLE = "- Doko-demo Rogue (MVP) -";
    public static final String START_HINT = "Press ENTER to descend";
    public static final String CONTROLS_HEADER = "Controls";
    public static final String CONTROLS_MOVE = "WASD / Arrows   Move (1 AP)";
    public static final String CONTROLS_SKILL = "1 - 9           Select card -> Arrow for direction";
    public static final String CONTROLS_WAIT = "SPACE           Wait (1 AP)";
    public static final String CONTROLS_END = "ENTER           End your turn";

    public static final String HUD_HP = "HP";
    public static final String HUD_AP = "AP";
    public static final String HUD_SOUL = "Soul";
    public static final String HUD_PHASE = "Phase";
    public static final String HUD_SPEED = "Speed";
    public static final String HUD_HINT =
        "WASD/Arrows: Move   1-9: Card   SPACE: Wait   ENTER: End turn";

    // Move token hint during movement mode (ADR-21 §15-5)
    public static final String MOVE_TOKEN_HINT = "Move tokens: use WASD/Arrows to move";

    public static final String PHASE_PLAYER = "YOUR TURN";
    public static final String PHASE_ENEMY = "ENEMY TURN";
    public static final String PHASE_GAMEOVER = "GAME OVER";
    public static final String PHASE_CLEARED = "CLEARED";

    public static final String GAME_OVER_HEADER = "YOU DIED";
    public static final String CLEARED_HEADER = "FLOOR CLEARED";
    public static final String SOULS_KEPT = "Souls carried out: ";
    public static final String NEW_RUN_HINT = "Press ENTER to start a new run";

    // Hint shown after stepping on stairs (§15-6 / §15-8 / E-6).
    // Popup overlays the screen; HUD hint area also shows the same info for redundancy.
    public static final String CLEARED_HINT = "Layer-end node: press 1, 2, or 3";

    // Title shown on the layer-end node selection popup (§15-8 / E-6)
    public static final String LAYER_END_TITLE = "Layer-end node — choose one";

    // 手札表示ラベル
    public static final String HAND_LABEL = "Hand:";
    public static final String HAND_HINT = "Number: select card  Arrow: direction  ESC: cancel";
    public static final String CARD_ELEMENT_PHYSICAL = "Physical";
    public static final String CARD_ELEMENT_MAGICAL = "Magical";

    // 移動権ラベル (ADR-21 §15-5: remaining free-move steps granted by a Move card)
    public static final String MOVE_TOKEN_REMAINING_FORMAT = "Move tokens: %d";

    public static final String EV_MOVED_FORMAT = "%s moved to (%d, %d)";
    public static final String EV_SKILL_USED_FORMAT = "%s used %s";
    public static final String EV_DAMAGE_FORMAT = "%s -> %s : %d damage (HP %d left)";
    public static final String EV_DIED_FORMAT = "%s died";
    public static final String EV_SOUL_GAINED_FORMAT = "%s gained %d soul";
    public static final String EV_PHASE_FORMAT = "Phase -> %s";
    public static final String EV_REJECTED_FORMAT = "%s: rejected (%s)";
    public static final String EV_MOVEMENT_GRANTED_FORMAT = "%s gains %d move step(s)";
    public static final String EV_TRAP_PLACED_FORMAT = "%s placed a trap at (%d, %d)";
    public static final String EV_TRAP_TRIGGERED_FORMAT = "%s stepped on a trap: %d damage (HP %d left)";
    public static final String EV_FLOOR_ADVANCED_FORMAT = "Reached floor %d";
    /** Buff applied log: %s = ActorId, %s = BuffKind label, %+d = signed amount, %d = remaining. */
    public static final String EV_BUFF_APPLIED_FORMAT = "%s buffed: %s %+d (%d turns left)";

    /** BuffKind English label. */
    public static final String BUFF_KIND_PHYSICAL_ATTACK = "PhyAtk";
    public static final String BUFF_KIND_MAGICAL_ATTACK = "MagAtk";
    public static final String BUFF_KIND_PHYSICAL_DEFENSE = "PhyDef";
    public static final String BUFF_KIND_MAGICAL_DEFENSE = "MagDef";
    public static final String BUFF_KIND_SPEED = "Speed";

    // §15-2 / §15-9 Gold (run-local currency)
    public static final String HUD_GOLD = "Gold";
    public static final String EV_GOLD_GAINED_FORMAT = "%s gained %d gold";

    // §15-3 / §15-6 Elite defeated notification
    public static final String EV_ELITE_DEFEATED_FORMAT = "Elite defeated: %s! Card reward unlocked";

    // §15-7 / E-2 Soul Tree screen / title screen entry
    public static final String SOUL_COST_FORMAT = "Soul %d";
    public static final String SOUL_TREE_TITLE = "Soul Tree";
    public static final String SOUL_TREE_INVENTORY_FORMAT = "Owned Soul: %d";
    public static final String SOUL_TREE_CONTROLS_HINT =
        "[Click] Unlock   [R] Reset (full refund)   [ESC] Back to Title";
    public static final String SOUL_TREE_FLASH_RESET = "Tree reset";
    public static final String TITLE_OPEN_TREE_HINT_FORMAT = "[T] Open Soul Tree (Soul: %d)";

    // §15-10 / E-10 Tutorial
    public static final String TUTORIAL_TITLE = "How to Play";
    public static final String TUTORIAL_BODY =
        """
        WASD / Arrows  : Move 1 tile (1 AP)
        1 - 9          : Select a card from hand
        Arrows (after) : Pick a direction for the card
        SPACE          : Wait 1 turn
        ENTER          : End your turn

        Reach the yellow stairs to descend.
        Defeat enemies to earn Soul + Gold.
        At each layer end, pick a node: stat / shop / event.""";
    public static final String TUTORIAL_CLOSE_HINT = "Press ENTER or ESC to close";
  }
}
