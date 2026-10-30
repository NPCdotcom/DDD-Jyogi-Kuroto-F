# カード画像の意味的整合監査

対象: 依頼で指定された local main (60ac172) の読み取り専用書き出しコピー。監査日: 2026-10-30（JST）。

参照ルート: `{S}\main`。リポジトリ本体の古い作業ツリーは参照していない。コミット番号は依頼情報に基づき、Git 履歴の照合は行っていない。

## 結果

- 59 カード + spare 1 枚の計 60 枚を、画像閲覧ツール view_image で個別に目視した。未確認 0 枚。
- 不一致・不足フラグ付きは 58/59 カード（重複除外）。内訳は名称 7、AP 36、レアリティ 28、効果文 53 件。種類間の重複あり。
- 効果文 53 件は、全文未掲載 48 件と部分未掲載 5 件。効果数値の矛盾は 0 件。名称・AP・レアリティの少なくとも一つに確認できる差があるカードは 44 枚。
- アートと効果の意味的判定（59 カード）: OK 54、疑義 3、不一致 2。spare は比較対象がないため、この集計から除外。
- 判読不可は 1 枚・1 箇所（card_32 の名称先頭 1 字）。その箇所の名称一致／不一致は保留。
- 現在の割当 PNG のままでは意味面で 5 枚に問題・疑義がある。既存の別カードのアートを共用すれば、60 枚ともアートの切り出し流用候補となり、新規生成が必須と確認できたものは 0 枚。

## 比較・転記・再利用の基準

- 画像欄は実際に見えた文字を転記し、アイコンは見えた形を記述した。名称・コストを JSON から補完していない。コストの数字には AP という英字が焼き込まれていないものもあり、コスト欄の数字として比較した。改行は <br>、空白は読みやすさのため正規化している。
- 画像の C / U / R / RARE は、監査上 COMMON / UNCOMMON / RARE / RARE と対応させた。画像自体には略号しかない場合がある。右上と下部が矛盾する場合は両方を残し、片方でも実データと違えばレアリティ差に数えた。
- rarity が JSON にない場合は「未指定→COMMON」と記した。Card.rarityOrDefault() の既定値に従う。tag と element は JSON の列を保持し、剣・杖などの図を根拠に英語の種別文字が書かれているとは扱っていない。
- 効果文フラグは「異なる記述」だけでなく、全文欠落と機械的情報の省略を含む。ダメージ属性（物理／魔法）、射程、範囲、効果量、継続時間を比較した。「移動距離 3」と「3 マス移動」、「魔防 +3 / 2 ターン」と「魔防 +3 (2T)」など、意味の同じ言い換え・区切りは不一致にしない。
- 部分不足は card_51・52 のダメージ属性、card_53・54 のダメージ属性と射程、card_58 の罠ダメージ属性。数値が同じでも属性や射程が画像の説明にないことを区別している。
- 「記載なし」はその欄が画像に存在しないという確認結果。「判読不可」は文字が見えるが確定できない箇所。「未確認」は画像自体を閲覧できない場合用で、今回の該当はない。
- 意味的整合と再利用可否は目視に基づく監査判断。OK は名称・基本効果の主題に矛盾がないという意味であり、威力、射程、ターン数までアートだけで証明するものではない。名称や絵の火・氷・毒・拘束・貫通等から、JSON にない状態異常や追加処理が実装済みとは判断していない。
- 再利用区分は drawCardComposite のアート入力に使う場合で判定。枠・コスト・文字まで含む完成カード画像なので、図柄が正しくても「そのまま可」にはしない。中央の絵だけを切り出し、縦横比を維持して縮小し、必要に応じ横方向の余白を加えて約 108×72 の枠へ収める想定。正方形に近い絵を無理に 3:2 へ引き伸ばしたり、剣・人物の先端を切る前提ではない。
- 再利用欄に別 PNG を記した 5 件は既存素材の共用を提案するもの。割当変更や成果物用のクロップは実装していない。絵柄の重複を許容しない追加要件がある場合は、その 5 件は再生成候補になる。
- PNG の寸法は元ファイルの IHDR を読み取った。目視は card_01 を元画像、残り 59 枚を元画像から最近傍で整数倍拡大した一時 PNG で実施した。拡大は情報を補間・生成しない。元ファイルは変更していない。

## 全 59 枚 + spare 1 枚

| cardId | png | 元画像寸法・縦横比（幅÷高さ） | 画像に焼き込まれた名称 / コスト / レアリティ・種別 / 効果文 | cards.json の名称・apCost・rarity・element・効果（tag 併記） | 不一致フラグ | アートと効果の意味的整合 | 再利用区分・使用するアート |
|---|---|---|---|---|---|---|---|
| stone_throw | [card_01.png](../../../assets/cards/card_01.png) | 150×188 px; 150:188; 0.797872 | 名称「石投げ」<br>コスト「1」<br>レア「C」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」<br>補記「なし」 | 名称「礫投げ」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":3,"range":3}<br>実装説明「物理ダメージ 3 (射程 3)」 | 名称 / 効果文<br>効果文: 効果文全文が未掲載 | OK — 飛翔する石の図柄で、物理の投石攻撃と整合。<br>目視: 中央に飛翔する石と速度線。 | アートのみクロップ流用可<br>card_01.png 自身の中央アート |
| heavy_slash | [card_02.png](../../../assets/cards/card_02.png) | 138×188 px; 138:188; 0.734043 | 名称「重斬り」<br>コスト「3」<br>レア「C」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「重斬り」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":6}<br>実装説明「物理ダメージ 6」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 大きな剣の図柄で、重い物理攻撃と整合。<br>目視: 単独の剣。 | アートのみクロップ流用可<br>card_02.png 自身の中央アート |
| quick_stab | [card_03.png](../../../assets/cards/card_03.png) | 134×186 px; 134:186; 0.720430 | 名称「速突き」<br>コスト「2」<br>レア「C」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「速突き」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「物理ダメージ 4」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 細身の刃は物理の刺突攻撃と整合。<br>目視: 単独の細身の剣。 | アートのみクロップ流用可<br>card_03.png 自身の中央アート |
| double_strike | [card_04.png](../../../assets/cards/card_04.png) | 138×184 px; 138:184; 0.750000 | 名称「二連撃」<br>コスト「2」<br>レア「U」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「二連撃」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":5}<br>実装説明「物理ダメージ 5」 | レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 二振りの剣と軌跡は二連撃の攻撃主題に合う。JSON に多段回数はない。<br>目視: 二振りの剣と青い弧。 | アートのみクロップ流用可<br>card_04.png 自身の中央アート |
| armor_break | [card_05.png](../../../assets/cards/card_05.png) | 142×183 px; 142:183; 0.775956 | 名称「鎧砕き」<br>コスト「4」<br>レア「U」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「鎧砕き」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":5}<br>実装説明「物理ダメージ 5」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 盾を砕く斧は物理攻撃の主題に合う。防御低下効果は JSON にない。<br>目視: 盾に食い込む斧と破片。 | アートのみクロップ流用可<br>card_05.png 自身の中央アート |
| overhead_smash | [card_06.png](../../../assets/cards/card_06.png) | 141×183 px; 141:183; 0.770492 | 名称「頭上強打」<br>コスト「5」<br>レア「R」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「振り下ろし」; apCost=3; rarity=UNCOMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":9}<br>実装説明「物理ダメージ 9」 | 名称 / AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 武器を頭上に振り上げる人物は振り下ろしの物理攻撃と整合。<br>目視: 緑の人型が武器を振り上げ、足元に橙色の軌跡。 | アートのみクロップ流用可<br>card_06.png 自身の中央アート |
| riposte | [card_07.png](../../../assets/cards/card_07.png) | 143×183 px; 143:183; 0.781421 | 名称「カウンター」<br>コスト「2」<br>レア「U」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「受け流し」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「物理ダメージ 4」 | 名称 / AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 交差して受け止める剣は受け流し・反撃の物理攻撃という主題に合う。<br>目視: 交差する二振りの剣。 | アートのみクロップ流用可<br>card_07.png 自身の中央アート |
| leg_sweep | [card_08.png](../../../assets/cards/card_08.png) | 136×184 px; 136:184; 0.739130 | 名称「足払い」<br>コスト「2」<br>レア「C」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「足払い」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":2}<br>実装説明「物理ダメージ 2」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 低い蹴りと足元の軌跡は足払いの物理攻撃と整合。<br>目視: 低い蹴りを繰り出す緑の人型。 | アートのみクロップ流用可<br>card_08.png 自身の中央アート |
| piercing_arrow | [card_09.png](../../../assets/cards/card_09.png) | 139×189 px; 139:189; 0.735450 | 名称「貫通矢」<br>コスト「3」<br>レア「U」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「貫通矢」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":6,"range":4}<br>実装説明「物理ダメージ 6 (射程 4)」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 飛翔する矢は射程4の物理攻撃と整合。貫通処理自体は JSON にない。<br>目視: 赤い矢羽根の飛翔する矢。 | アートのみクロップ流用可<br>card_09.png 自身の中央アート |
| skull_crack | [card_10.png](../../../assets/cards/card_10.png) | 140×186 px; 140:186; 0.752688 | 名称「頭蓋砕き」<br>コスト「6」<br>レア「R」<br>種別: 交差した剣アイコン（文字表記なし）<br>効果「記載なし」 | 名称「頭蓋割り」; apCost=3; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":8}<br>実装説明「物理ダメージ 8」 | 名称 / AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 頭部に棘付き棍棒が当たる図は頭蓋割りの物理攻撃と整合。<br>目視: 頭部シルエットへの棍棒の打撃。 | アートのみクロップ流用可<br>card_10.png 自身の中央アート |
| ice_lance | [card_11.png](../../../assets/cards/card_11.png) | 138×189 px; 138:189; 0.730159 | 名称「氷槍」<br>コスト「2」<br>レア「C」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「氷槍」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":6,"range":4}<br>実装説明「魔法ダメージ 6 (射程 4)」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 氷の槍は射程4の魔法攻撃という主題と整合。<br>目視: 青白い氷の槍。上端に隣接素材の断片状の緑線あり。 | アートのみクロップ流用可<br>card_11.png 自身の中央アート |
| thunder_bolt | [card_12.png](../../../assets/cards/card_12.png) | 137×186 px; 137:186; 0.736559 | 名称「雷撃」<br>コスト「4」<br>レア「U」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「雷撃」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":7}<br>実装説明「魔法ダメージ 7」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 稲妻は雷撃の魔法ダメージと整合。<br>目視: 黄色の稲妻。上端に断片状の緑線あり。 | アートのみクロップ流用可<br>card_12.png 自身の中央アート |
| dark_pulse | [card_13.png](../../../assets/cards/card_13.png) | 141×185 px; 141:185; 0.762162 | 名称「暗黒波」<br>コスト「3」<br>レア「U」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「暗黒波」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「魔法ダメージ 4」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 紫の暗い波は暗黒波による魔法ダメージと整合。<br>目視: 紫色の巻く波。 | アートのみクロップ流用可<br>card_13.png 自身の中央アート |
| wind_blade | [card_14.png](../../../assets/cards/card_14.png) | 137×186 px; 137:186; 0.736559 | 名称「風刃」<br>コスト「1」<br>レア「C」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「風刃」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":2}<br>実装説明「魔法ダメージ 2」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 鋭い風の渦は風系の魔法攻撃の主題と整合。<br>目視: 青白い旋風。上端に断片状の緑線あり。 | アートのみクロップ流用可<br>card_14.png 自身の中央アート |
| frost_nova | [card_15.png](../../../assets/cards/card_15.png) | 134×185 px; 134:185; 0.724324 | 名称「凍結爆発」<br>コスト「6」<br>レア「R」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「凍結爆発」; apCost=3; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":8}<br>実装説明「魔法ダメージ 8」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 氷の放射状の閃光は氷系魔法攻撃と整合。凍結・範囲処理は JSON にない。<br>目視: 氷色の放射状の爆発。 | アートのみクロップ流用可<br>card_15.png 自身の中央アート |
| void_strike | [card_16.png](../../../assets/cards/card_16.png) | 139×181 px; 139:181; 0.767956 | 名称「虚無撃」<br>コスト「6」<br>レア「R」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「虚無撃」; apCost=3; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":9}<br>実装説明「魔法ダメージ 9」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 黒紫の魔力球は虚無系の魔法攻撃という主題に合う。<br>目視: 紫の輪を持つ黒い球。 | アートのみクロップ流用可<br>card_16.png 自身の中央アート |
| acid_splash | [card_17.png](../../../assets/cards/card_17.png) | 135×182 px; 135:182; 0.741758 | 名称「酸の飛沫」<br>コスト「2」<br>レア「C」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「酸の飛沫」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「魔法ダメージ 4」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 緑の液体の飛沫は酸による魔法ダメージの主題と整合。<br>目視: 緑色の飛び散る液体。 | アートのみクロップ流用可<br>card_17.png 自身の中央アート |
| holy_bolt | [card_18.png](../../../assets/cards/card_18.png) | 136×188 px; 136:188; 0.723404 | 名称「聖光弾」<br>コスト「3」<br>レア「U」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「聖光弾」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":5}<br>実装説明「魔法ダメージ 5」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 白金色の光球は聖光弾の魔法攻撃と整合。<br>目視: 白金色に発光する球。 | アートのみクロップ流用可<br>card_18.png 自身の中央アート |
| meteor_drop | [card_19.png](../../../assets/cards/card_19.png) | 139×188 px; 139:188; 0.739362 | 名称「隕石落とし」<br>コスト「6」<br>レア「R」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「隕石落とし」; apCost=3; rarity=RARE; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":10,"range":6,"areaRadius":2}<br>実装説明「魔法ダメージ 10 (射程 6 / 範囲 2)」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 炎を引く隕石は射程6・範囲2の隕石魔法攻撃と整合。<br>目視: 火炎の尾を引く隕石。 | アートのみクロップ流用可<br>card_19.png 自身の中央アート |
| shadow_bolt | [card_20.png](../../../assets/cards/card_20.png) | 139×179 px; 139:179; 0.776536 | 名称「影の矢」<br>コスト「1」<br>レア「C」<br>種別: 紫の宝玉付き杖アイコン（文字表記なし）<br>効果「記載なし」 | 名称「影の矢」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「魔法ダメージ 4」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 黒紫の飛翔体は影の矢という魔法攻撃の主題と整合。<br>目視: 黒紫の弾と尾。 | アートのみクロップ流用可<br>card_20.png 自身の中央アート |
| dash | [card_21.png](../../../assets/cards/card_21.png) | 140×179 px; 140:179; 0.782123 | 名称「ダッシュ」<br>コスト「1」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「ダッシュ」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=PHYSICAL<br>effect={"type":"Move","distance":2}<br>実装説明「2 マス移動」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 横方向の速度線は2マス移動のダッシュと整合。<br>目視: 青白い横向きの速度線。 | アートのみクロップ流用可<br>card_21.png 自身の中央アート |
| leap | [card_22.png](../../../assets/cards/card_22.png) | 138×181 px; 138:181; 0.762431 | 名称「跳躍」<br>コスト「2」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「跳躍」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=PHYSICAL<br>effect={"type":"Move","distance":3}<br>実装説明「3 マス移動」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 上へ弧を描く軌跡は跳躍による3マス移動と整合。<br>目視: 青白い上向きの弧状の軌跡。 | アートのみクロップ流用可<br>card_22.png 自身の中央アート |
| spare（未割当） | [card_23.png](../../../assets/cards/card_23.png) | 133×179 px; 133:179; 0.743017 | 名称「転がり」<br>コスト「2」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 該当なし。card_image_map.json の spareImages に card_23。roll は card_24 に割当済み。spare の表示値を roll のデータとして扱わない。 | なし（比較対象なし） | OK（参考・集計外） — 円弧状の軌跡は転がりの移動素材として解釈可能。spare のため対応効果は未割当。<br>目視: 青白い半円形の軌跡。 | アートのみクロップ流用可<br>card_23.png 自身の中央アート |
| roll | [card_24.png](../../../assets/cards/card_24.png) | 135×176 px; 135:176; 0.767045 | 名称「転がり」<br>コスト「2」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「回避転がり」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=PHYSICAL<br>effect={"type":"Move","distance":1}<br>実装説明「1 マス移動」 | 名称 / AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 地面を転がる人物は回避転がりの1マス移動と整合。回避無敵は JSON にない。<br>目視: 砂煙を上げ地面を転がる緑の人型。 | アートのみクロップ流用可<br>card_24.png 自身の中央アート |
| shadow_step | [card_25.png](../../../assets/cards/card_25.png) | 140×180 px; 140:180; 0.777778 | 名称「影歩き」<br>コスト「3」<br>レア「U」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「影歩き」; apCost=2; rarity=未指定→COMMON; tag=MOVEMENT; element=MAGICAL<br>effect={"type":"Move","distance":4}<br>実装説明「4 マス移動」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 暗紫の門は影を介する4マスの魔法移動と整合。<br>目視: 縦長の暗紫色の門。 | アートのみクロップ流用可<br>card_25.png 自身の中央アート |
| charge | [card_26.png](../../../assets/cards/card_26.png) | 136×177 px; 136:177; 0.768362 | 名称「突撃」<br>コスト「3」<br>レア「U」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「突撃」; apCost=2; rarity=未指定→COMMON; tag=MOVEMENT; element=PHYSICAL<br>effect={"type":"Move","distance":4}<br>実装説明「4 マス移動」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 前方へ走る人物は突撃の4マス物理移動と整合。攻撃処理は JSON にない。<br>目視: 橙色の速度線を背に前進する緑の人型。 | アートのみクロップ流用可<br>card_26.png 自身の中央アート |
| wind_walk | [card_28.png](../../../assets/cards/card_28.png) | 139×178 px; 139:178; 0.780899 | 名称「風走り」<br>コスト「2」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「風走り」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=MAGICAL<br>effect={"type":"Move","distance":2}<br>実装説明「2 マス移動」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 風をまとった足の速度線は風走りの2マス移動と整合。<br>目視: 青白い二本の風の軌跡・足状の図。 | アートのみクロップ流用可<br>card_28.png 自身の中央アート |
| retreat | [card_29.png](../../../assets/cards/card_29.png) | 134×178 px; 134:178; 0.752809 | 名称「後退」<br>コスト「1」<br>レア「C」<br>種別: 白い三つ又状の移動アイコン（文字表記なし）<br>効果「記載なし」 | 名称「後退」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=PHYSICAL<br>effect={"type":"Move","distance":2}<br>実装説明「2 マス移動」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 逆向きの矢印は後退の移動主題に合う。JSON は移動方向を規定しない。<br>目視: 左へ向く青い矢印と軌跡。 | アートのみクロップ流用可<br>card_29.png 自身の中央アート |
| teleport | [card_30.png](../../../assets/cards/card_30.png) | 133×171 px; 133:171; 0.777778 | 名称「テレポート」<br>コスト「4」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「瞬間移動」; apCost=3; rarity=UNCOMMON; tag=MOVEMENT; element=MAGICAL<br>effect={"type":"Move","distance":6}<br>実装説明「6 マス移動」 | 名称 / AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 魔法陣は瞬間移動の6マス魔法移動という主題に合う。<br>目視: 紫の幾何学的な魔法陣。 | アートのみクロップ流用可<br>card_30.png 自身の中央アート |
| iron_skin | [card_31.png](../../../assets/cards/card_31.png) | 135×181 px; 135:181; 0.745856 | 名称「鉄の皮膚」<br>コスト「3」<br>レア「右上 U／下部 U」<br>種別: 種別文字・アイコンなし（右上は U）<br>効果「記載なし」 | 名称「鉄の皮膚」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_DEFENSE_UP","amount":2,"durationTurns":3}<br>実装説明「物防 +2 (3T)」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 金属化した身体・鎧は物理防御上昇と整合。<br>目視: 鋼鉄色の胴鎧・人型の上半身。 | アートのみクロップ流用可<br>card_31.png 自身の中央アート |
| battle_cry | [card_32.png](../../../assets/cards/card_32.png) | 140×183 px; 140:183; 0.765027 | 名称「判読不可（先頭1字：鬨／閧の字形を確定できず）の声」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「鬨の声」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_ATTACK_UP","amount":2,"durationTurns":2}<br>実装説明「物攻 +2 (2T)」 | 効果文<br>効果文: 効果文全文が未掲載<br>比較保留: 名称（先頭漢字の字形） | OK — 叫ぶ人物と強いオーラは鬨の声による物理攻撃上昇と整合。<br>目視: 拳を握り叫ぶ緑の人型と黄色のオーラ。 | アートのみクロップ流用可<br>card_32.png 自身の中央アート |
| focus | [card_33.png](../../../assets/cards/card_33.png) | 138×187 px; 138:187; 0.737968 | 名称「集中」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「集中」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"MAGICAL_ATTACK_UP","amount":2,"durationTurns":2}<br>実装説明「魔攻 +2 (2T)」 | 効果文<br>効果文: 効果文全文が未掲載 | 疑義 — 主役の下向き矢印は能力低下にも見える。魔力を集める表現とも読め、魔攻+2との整合を断定できない。<br>目視: 魔法陣に向かう大きな青い下向き矢印。 | アートのみクロップ流用可<br>card_37.png（魔力の図柄へ差し替え） |
| magic_barrier | [card_34.png](../../../assets/cards/card_34.png) | 137×184 px; 137:184; 0.744565 | 名称「魔法障壁」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「魔法障壁」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"MAGICAL_DEFENSE_UP","amount":3,"durationTurns":2}<br>実装説明「魔防 +3 (2T)」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 魔力をまとった青い盾は魔法防御上昇と整合。<br>目視: 青い盾と青白い障壁。 | アートのみクロップ流用可<br>card_34.png 自身の中央アート |
| swiftness | [card_35.png](../../../assets/cards/card_35.png) | 135×182 px; 135:182; 0.741758 | 名称「迅速」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「疾風」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"SPEED_UP","amount":2,"durationTurns":3}<br>実装説明「速度 +2 (3T)」 | 名称 / AP / 効果文<br>効果文: 効果文全文が未掲載 | 疑義 — 速度線はあるが主役が盾で、防御強化を連想させる。速度+2を直接示す図柄ではない。<br>目視: 木の盾と橙色の速度線。 | アートのみクロップ流用可<br>card_61.png（羽付きの靴へ差し替え） |
| berserker | [card_36.png](../../../assets/cards/card_36.png) | 135×187 px; 135:187; 0.721925 | 名称「狂戦士」<br>コスト「5」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「狂戦士」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_ATTACK_UP","amount":4,"durationTurns":2}<br>実装説明「物攻 +4 (2T)」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 赤い筋肉質の戦士と怒りのオーラは物理攻撃上昇と整合。<br>目視: 赤い筋肉質の人型と赤いオーラ。 | アートのみクロップ流用可<br>card_36.png 自身の中央アート |
| arcane_surge | [card_37.png](../../../assets/cards/card_37.png) | 139×181 px; 139:181; 0.767956 | 名称「魔力放出」<br>コスト「5」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「魔力放出」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"MAGICAL_ATTACK_UP","amount":4,"durationTurns":2}<br>実装説明「魔攻 +4 (2T)」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 紫の魔力の放射は魔法攻撃上昇の主題に合う。<br>目視: 紫の中心球から放射する魔力。 | アートのみクロップ流用可<br>card_37.png 自身の中央アート |
| fortify | [card_38.png](../../../assets/cards/card_38.png) | 138×183 px; 138:183; 0.754098 | 名称「要塞化」<br>コスト「5」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「要塞化」; apCost=3; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_DEFENSE_UP","amount":5,"durationTurns":3}<br>実装説明「物防 +5 (3T)」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | 不一致 — 時計と速度線は時間・速度強化の図柄で、物理防御+5の要塞化を表していない。<br>目視: 時計と青緑の速度線。 | アートのみクロップ流用可<br>card_39.png（城壁へ差し替え） |
| mana_shield | [card_39.png](../../../assets/cards/card_39.png) | 139×183 px; 139:183; 0.759563 | 名称「マナシールド」<br>コスト「6」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「マナシールド」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"MAGICAL_DEFENSE_UP","amount":2,"durationTurns":3}<br>実装説明「魔防 +2 (3T)」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | 疑義 — 石造りの城壁は物理的な要塞に見える。防御主題は合うが魔法防御のマナシールドとは識別しづらい。<br>目視: 門と城壁・尖塔。 | アートのみクロップ流用可<br>card_34.png（魔法の盾へ差し替え） |
| war_stance | [card_40.png](../../../assets/cards/card_40.png) | 139×187 px; 139:187; 0.743316 | 名称「戦闘態勢」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「戦闘態勢」; apCost=1; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_ATTACK_UP","amount":2,"durationTurns":4}<br>実装説明「物攻 +2 (4T)」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | 不一致 — 主役が発光する盾で防御強化を示すが、実データは物理攻撃+2のみ。<br>目視: 青い盾と橙色のオーラ。 | アートのみクロップ流用可<br>card_32.png（気勢を上げる戦士へ差し替え） |
| spike_trap | [card_41.png](../../../assets/cards/card_41.png) | 138×180 px; 138:180; 0.766667 | 名称「棘罠」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「棘罠」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=PHYSICAL<br>effect={"type":"Trap","baseValue":4,"lifetime":{"type":"UntilStepped"}}<br>実装説明「罠 物理 4 / 踏むまで」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 床の金属棘は踏むまで残る物理ダメージ罠と整合。<br>目視: 床板から立つ複数の金属棘。 | アートのみクロップ流用可<br>card_41.png 自身の中央アート |
| bear_trap | [card_42.png](../../../assets/cards/card_42.png) | 138×180 px; 138:180; 0.766667 | 名称「熊罠」<br>コスト「2」<br>レア「右上 U／下部 C（画像内で矛盾）」<br>種別: 種別文字・アイコンなし（右上は U）<br>効果「記載なし」 | 名称「熊罠」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=PHYSICAL<br>effect={"type":"Trap","baseValue":5,"lifetime":{"type":"UntilStepped"}}<br>実装説明「罠 物理 5 / 踏むまで」 | レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 開いた金属製の挟み罠は踏むまで残る熊罠と整合。<br>目視: 歯のある金属製の挟み罠。 | アートのみクロップ流用可<br>card_42.png 自身の中央アート |
| poison_needle | [card_43.png](../../../assets/cards/card_43.png) | 133×179 px; 133:179; 0.743017 | 名称「毒針罠」<br>コスト「1」<br>レア「下部 C」<br>種別: 輪と二股状の白いアイコン（文字表記なし）<br>効果「記載なし」 | 名称「毒針罠」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=PHYSICAL<br>effect={"type":"Trap","baseValue":4,"lifetime":{"type":"UntilStepped"}}<br>実装説明「罠 物理 4 / 踏むまで」 | AP / 効果文<br>効果文: 効果文全文が未掲載 | OK — 毒液をまとった針は毒針罠の主題と整合。継続毒は JSON にない。<br>目視: 緑の液体をまとった鋭い針。 | アートのみクロップ流用可<br>card_43.png 自身の中央アート |
| pitfall | [card_44.png](../../../assets/cards/card_44.png) | 137×175 px; 137:175; 0.782857 | 名称「落とし穴」<br>コスト「4」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「落とし穴」; apCost=3; rarity=未指定→COMMON; tag=TRAP; element=PHYSICAL<br>effect={"type":"Trap","baseValue":7,"lifetime":{"type":"UntilStepped"}}<br>実装説明「罠 物理 7 / 踏むまで」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 地面の穴は踏むまで残る落とし穴の物理罠と整合。<br>目視: 地面の暗い穴と縁の土。 | アートのみクロップ流用可<br>card_44.png 自身の中央アート |
| wire_trip | [card_45.png](../../../assets/cards/card_45.png) | 137×182 px; 137:182; 0.752747 | 名称「鉄線罠」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「鉄線罠」; apCost=1; rarity=未指定→COMMON; tag=TRAP; element=PHYSICAL<br>effect={"type":"Trap","baseValue":2,"lifetime":{"type":"UntilStepped"}}<br>実装説明「罠 物理 2 / 踏むまで」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 交差する鉄線は物理的な鉄線罠と整合。<br>目視: 交差する二本の鉄線。 | アートのみクロップ流用可<br>card_45.png 自身の中央アート |
| frost_rune | [card_46.png](../../../assets/cards/card_46.png) | 136×178 px; 136:178; 0.764045 | 名称「氷結ルーン」<br>コスト「3」<br>レア「右上 U／下部 U」<br>種別: 種別文字・アイコンなし（右上は U）<br>効果「記載なし」 | 名称「氷結ルーン」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":4,"lifetime":{"type":"Turns","remaining":4}}<br>実装説明「罠 魔法 4 / 4T 残」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 青い幾何学的なルーンは氷結ルーンの魔法罠という主題と整合。凍結状態付与は JSON にない。<br>目視: 青白い三角形のルーン。 | アートのみクロップ流用可<br>card_46.png 自身の中央アート |
| thunder_rune | [card_47.png](../../../assets/cards/card_47.png) | 137×176 px; 137:176; 0.778409 | 名称「雷撃ルーン」<br>コスト「3」<br>レア「右上 U／下部 U」<br>種別: 種別文字・アイコンなし（右上は U）<br>効果「記載なし」 | 名称「雷撃ルーン」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":4,"lifetime":{"type":"Turns","remaining":3}}<br>実装説明「罠 魔法 4 / 3T 残」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 稲妻形の記号は雷撃ルーンの魔法罠という主題に合う。<br>目視: 金色の稲妻形の紋章。 | アートのみクロップ流用可<br>card_47.png 自身の中央アート |
| void_rune | [card_48.png](../../../assets/cards/card_48.png) | 138×182 px; 138:182; 0.758242 | 名称「虚無ルーン」<br>コスト「5」<br>レア「右上 R／下部 R」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「記載なし」 | 名称「虚無ルーン」; apCost=3; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":7,"lifetime":{"type":"Turns","remaining":5}}<br>実装説明「罠 魔法 7 / 5T 残」 | AP / レアリティ / 効果文<br>効果文: 効果文全文が未掲載 | OK — 黒紫の異形の紋章は虚無ルーンの魔法罠という主題に合う。<br>目視: 紫の輪に囲まれた黒い眼状の紋章。 | アートのみクロップ流用可<br>card_48.png 自身の中央アート |
| acid_pool | [card_49.png](../../../assets/cards/card_49.png) | 138×179 px; 138:179; 0.770950 | 名称「酸の水たまり」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「酸の水たまり」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":4,"lifetime":{"type":"Turns","remaining":3}}<br>実装説明「罠 魔法 4 / 3T 残」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 緑の液体の水たまりは3T残る酸の魔法罠と整合。<br>目視: 緑色の水たまりと液滴。名称が枠外にはみ出す。 | アートのみクロップ流用可<br>card_49.png 自身の中央アート |
| dark_snare | [card_50.png](../../../assets/cards/card_50.png) | 134×178 px; 134:178; 0.752809 | 名称「闇の罠」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「記載なし」 | 名称「闇の罠」; apCost=1; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":2,"lifetime":{"type":"Turns","remaining":3}}<br>実装説明「罠 魔法 2 / 3T 残」 | 効果文<br>効果文: 効果文全文が未掲載 | OK — 黒い拘束具は闇の罠の主題に合う。実効果は魔法ダメージで、移動不能は JSON にない。<br>目視: 黒い絡み合う縄・拘束具状の輪。 | アートのみクロップ流用可<br>card_50.png 自身の中央アート |
| zangeki | [card_51.png](../../../assets/cards/card_51.png) | 198×282 px; 198:282; 0.702128 | 名称「斬撃」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「ダメージ 3」<br>補記「(初期カード)」 | 名称「斬撃」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":3}<br>実装説明「物理ダメージ 3」 | 効果文<br>効果文: ダメージの物理/魔法属性が未掲載 | OK — 剣は物理ダメージ3の斬撃と整合。<br>目視: 単独の剣。外周に市松模様。 | アートのみクロップ流用可<br>card_51.png 自身の中央アート |
| strong_strike | [card_52.png](../../../assets/cards/card_52.png) | 196×286 px; 196:286; 0.685315 | 名称「強打」<br>コスト「1」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「ダメージ 4」 | 名称「強打」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=PHYSICAL<br>effect={"type":"Damage","baseValue":4}<br>実装説明「物理ダメージ 4」 | 効果文<br>効果文: ダメージの物理/魔法属性が未掲載 | OK — 拳の打撃と閃光は物理ダメージ4の強打と整合。<br>目視: 籠手の拳と橙の打撃光。外周に市松模様。 | アートのみクロップ流用可<br>card_52.png 自身の中央アート |
| magic_bolt | [card_53.png](../../../assets/cards/card_53.png) | 190×280 px; 190:280; 0.678571 | 名称「魔法弾」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「ダメージ 5」 | 名称「魔法弾」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":5,"range":4}<br>実装説明「魔法ダメージ 5 (射程 4)」 | 効果文<br>効果文: ダメージの物理/魔法属性と射程が未掲載 | OK — 青い魔力球は射程4の魔法攻撃と整合。画像の説明には射程の記載がない。<br>目視: 青い発光弾。外周に市松模様。 | アートのみクロップ流用可<br>card_53.png 自身の中央アート |
| fireball | [card_54.png](../../../assets/cards/card_54.png) | 202×290 px; 202:290; 0.696552 | 名称「火球」<br>コスト「2」<br>レア「右上 U／下部 U」<br>種別: 種別文字・アイコンなし（右上は U）<br>効果「ダメージ 6」 | 名称「火球」; apCost=2; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":6,"range":5}<br>実装説明「魔法ダメージ 6 (射程 5)」 | レアリティ / 効果文<br>効果文: ダメージの物理/魔法属性と射程が未掲載 | OK — 火の球は射程5の魔法ダメージ6という主題と整合。画像の説明には射程の記載がない。<br>目視: 橙色の火球。外周に市松模様。 | アートのみクロップ流用可<br>card_54.png 自身の中央アート |
| ember_shot | [card_55.png](../../../assets/cards/card_55.png) | 786×1169 px; 786:1169; 0.672370 | 名称「炎弾」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「魔法ダメージ 3」 | 名称「炎弾」; apCost=1; rarity=未指定→COMMON; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":3}<br>実装説明「魔法ダメージ 3」 | AP | OK — 燃える弾は魔法ダメージ3の炎弾と整合。<br>目視: 炎をまとった丸い弾。 | アートのみクロップ流用可<br>card_55.png 自身の中央アート |
| blaze_nova | [card_56.png](../../../assets/cards/card_56.png) | 807×1186 px; 807:1186; 0.680438 | 名称「爆炎」<br>コスト「5」<br>レア「下部 RARE（右上・下部に銀色の盾状バッジ）」<br>種別: 種別文字なし（盾状バッジはレアリティ表示に付随）<br>効果「魔法ダメージ 9 / 射程 4 / 範囲 1」 | 名称「爆炎」; apCost=3; rarity=RARE; tag=ATTACK; element=MAGICAL<br>effect={"type":"Damage","baseValue":9,"range":4,"areaRadius":1}<br>実装説明「魔法ダメージ 9 (射程 4 / 範囲 1)」 | AP | OK — 炎の大きな爆発は射程4・範囲1の魔法ダメージ9と整合。<br>目視: 大きな炎の渦と爆発。 | アートのみクロップ流用可<br>card_56.png 自身の中央アート |
| blink_step | [card_57.png](../../../assets/cards/card_57.png) | 200×290 px; 200:290; 0.689655 | 名称「瞬歩」<br>コスト「1」<br>レア「右上 R／下部 C（画像内で矛盾）」<br>種別: 種別文字・アイコンなし（右上は R）<br>効果「移動距離 3」 | 名称「瞬歩」; apCost=1; rarity=未指定→COMMON; tag=MOVEMENT; element=MAGICAL<br>effect={"type":"Move","distance":3}<br>実装説明「3 マス移動」 | レアリティ | OK — 残像を残す人影は瞬歩の3マス魔法移動と整合。<br>目視: 紫の残像を背に走る黒い人影。外周に市松模様。 | アートのみクロップ流用可<br>card_57.png 自身の中央アート |
| flame_circle | [card_58.png](../../../assets/cards/card_58.png) | 200×294 px; 200:294; 0.680272 | 名称「炎の魔法陣」<br>コスト「2」<br>レア「下部 U」<br>種別: 右上に下向き矢印と床状のアイコン（文字表記なし）<br>効果「トラップ5 ダメ / 4 ターン持続」 | 名称「炎の魔法陣」; apCost=2; rarity=未指定→COMMON; tag=TRAP; element=MAGICAL<br>effect={"type":"Trap","baseValue":5,"lifetime":{"type":"Turns","remaining":4}}<br>実装説明「罠 魔法 5 / 4T 残」 | レアリティ / 効果文<br>効果文: ダメージの物理/魔法属性が未掲載 | OK — 火の輝きは炎の罠の主題に合う。設置図形は弱いが罠アイコンと説明が補う。<br>目視: 橙色の星形の火炎光。外周に市松模様。 | アートのみクロップ流用可<br>card_58.png 自身の中央アート |
| arcane_veil | [card_59.png](../../../assets/cards/card_59.png) | 198×296 px; 198:296; 0.668919 | 名称「魔力の帳」<br>コスト「2」<br>レア「右上 C／下部 C」<br>種別: 種別文字・アイコンなし（右上は C）<br>効果「魔防 +3 / 2 ターン」 | 名称「魔力の帳」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"MAGICAL_DEFENSE_UP","amount":3,"durationTurns":2}<br>実装説明「魔防 +3 (2T)」 | なし | OK — 魔力の核を囲む防護膜・盾状の図は魔法防御+3と整合。<br>目視: 紫の核を持つ白紫色の防護膜・盾。外周に市松模様。 | アートのみクロップ流用可<br>card_59.png 自身の中央アート |
| stone_wall | [card_60.png](../../../assets/cards/card_60.png) | 204×296 px; 204:296; 0.689189 | 名称「岩壁の構え」<br>コスト「2」<br>レア「下部 U」<br>種別: 右上に青い上向き矢印（文字表記なし）<br>効果「物防 +4 / 2 ターン」 | 名称「岩壁の構え」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=PHYSICAL<br>effect={"type":"Buff","kind":"PHYSICAL_DEFENSE_UP","amount":4,"durationTurns":2}<br>実装説明「物防 +4 (2T)」 | レアリティ | OK — 積まれた石壁は物理防御+4の岩壁の構えと整合。<br>目視: 四枚の石板でできた壁。外周に市松模様。 | アートのみクロップ流用可<br>card_60.png 自身の中央アート |
| haste | [card_61.png](../../../assets/cards/card_61.png) | 196×296 px; 196:296; 0.662162 | 名称「ヘイスト」<br>コスト「2」<br>レア「下部 U」<br>種別: 右上に青い上向き矢印（文字表記なし）<br>効果「速度 +3 / 3 ターン」 | 名称「ヘイスト」; apCost=2; rarity=未指定→COMMON; tag=BUFF; element=MAGICAL<br>effect={"type":"Buff","kind":"SPEED_UP","amount":3,"durationTurns":3}<br>実装説明「速度 +3 (3T)」 | レアリティ | OK — 羽の付いた靴は速度+3のヘイストと整合。<br>目視: 青白い羽の付いた茶色の靴。外周に市松模様。 | アートのみクロップ流用可<br>card_61.png 自身の中央アート |

## 集計

フラグは 59 枚の対応カードを母数とし、spare は除外する。種類間の重複がある。

| 不一致・不足の種類 | 件数 | 詳細 |
|---|---:|---|
| 名称 | 7 | stone_throw / overhead_smash / riposte / skull_crack / roll / teleport / swiftness。battle_cry の先頭漢字は判定保留で加算しない。 |
| AP | 36 | 表の画像コストと apCost の数値差。 |
| レアリティ | 28 | 画像内の上下矛盾も含む。bear_trap は U/C、blink_step は R/C。 |
| 効果文（欠落を含む） | 53 | 全文未掲載 48、部分未掲載 5。効果量・移動距離・継続時間・射程・範囲の明記された数値自体の矛盾 0。 |
| いずれかのフラグあり（重複除外） | 58 | 画像の情報を完成カードとして厳密に比較。 |
| 確認できる名称・AP・レアリティ差あり（重複除外） | 44 | 効果文の欠落だけによる差を除いた件数。 |
| フラグなし | 1 | arcane_veil（card_59）。合成描画用にはアートの切り出しが必要。 |

| 目視・意味的判定 | 59 カード | spare | 合計 |
|---|---:|---:|---:|
| OK | 54 | 1（比較対象なしの参考評価） | 55 |
| 疑義 | 3 | 0 | 3 |
| 不一致 | 2 | 0 | 2 |
| 判読不可がある画像 | 1 | 0 | 1 |
| 判読不可の箇所 | 1 | 0 | 1 |
| 未確認 | 0 | 0 | 0 |
| 効果文の記載なし | 48 | 1 | 49 |

判読不可の原文範囲は card_32 の「［先頭 1 字判読不可］の声」。字形は「鬨／閧」の区別ができず、JSON の「鬨の声」を画像の転記として採用していない。

### 寸法・縦横比のばらつき（60 枚）

| 指標 | 最小 | 最大 | 中央値 |
|---|---:|---:|---:|
| 幅（px） | 133 | 807 | 138.0 |
| 高さ（px） | 171 | 1186 | 183.0 |
| 幅÷高さ | 0.662162 | 0.797872 | 0.747928 |

- 幅と高さの組合せは 54 種類。最小幅 133 px は card_23・30・43、最小高 171 px は card_30。幅・高さの最大は card_56（807×1186）。
- 最も縦長: card_61.png（196×296、比 0.662162）。最も横に広い: card_01.png（150×188、比 0.797872）。
- 49 枚は幅 133〜150・高さ 171〜189 px、9 枚は幅 190〜204・高さ 280〜296 px、2 枚（card_55・56）は幅 786〜807・高さ 1169〜1186 px。元画像の解像度には約 6 倍の幅差がある。
- drawCardComposite の基準サイズ 120×168、borderWidth=3 ではアート幅 120−3×4=108、高さ 72 で比は 1.5。元画像はすべて幅÷高さが 0.80 未満の縦長。全体をアート枠へ入れると枠・名称・コストが重複し、図柄の比率も崩れる。

## 既存素材の再利用で解決できる範囲

### 既存の別カードからアートを共用してよい場合（表の再利用区分）

| 区分 | 59 カード | spare | 合計 |
|---|---:|---:|---:|
| そのまま可 | 0 | 0 | 0 |
| アートのみクロップ流用可 | 59 | 1 | 60 |
| 要再生成 | 0 | 0 | 0 |

5 件は次の差し替え提案を含む。提案先の PNG も個別に目視済みで、枠・説明を除いた絵だけを使う。

| 対象 cardId | 現割当の問題 | 既存アート候補 | 判断 |
|---|---|---|---|
| focus | card_33 の下向き矢印は強化と逆にも見える | card_37 の紫の魔力 | 魔法攻撃強化という効果種が同じ。量・期間の文字はコード側で出す。 |
| swiftness | card_35 の主役が盾 | card_61 の羽付き靴 | 速度上昇を直接連想できる。物理／魔法の区別は JSON の表示へ任せる。 |
| fortify | card_38 が時計と速度線 | card_39 の城壁 | 物理防御の要塞化を表す。 |
| mana_shield | card_39 が物理的な城壁に見える | card_34 の青い魔法の盾 | 魔法防御上昇という効果種が同じ。 |
| war_stance | card_40 が盾で防御に見える | card_32 の気勢を上げる戦士 | 物理攻撃上昇という効果種が同じ。名称先頭の判読不可は切り出しで除ける。 |

### 現割当 PNG だけを使い、疑義のある図柄を置き換える場合

| 区分 | 59 カード | spare | 合計 |
|---|---:|---:|---:|
| そのまま可 | 0 | 0 | 0 |
| アートのみクロップ流用可 | 54 | 1 | 55 |
| 要再生成・アート差替え候補 | 5 | 0 | 5 |

この 5 枚は focus / swiftness / fortify / mana_shield / war_stance。意味的不一致 2 枚と疑義 3 枚を合わせたもの。既存別素材の共用を許容すれば上の 0 枚へ減らせるため、有料 API や新規画像生成が必要だという結論ではない。

再利用時は絵の外の文字・コスト・レアリティバッジと外周の市松模様・隣接断片を除く。card_51〜54・57〜61 の外周には市松模様が見えるが、その透明性は外観だけで断定していない。card_55・56 は解像度が高いため同じ基準アート枠へ縮小する。最終的な切り出し矩形、余白、縮小方式、実機の見え方の確認はこの読み取り専用監査の後工程であり、今回は実施していない。

## 参照実装と検証

- [cards.json](../../../src/main/resources/cards.json): 59 件の元データ。[card_image_map.json](../../../src/main/resources/card_image_map.json): 59 個の一意な対応、spareImages=card_23、未対応 0。
- [Card.java](../../../src/main/java/core/domain/card/Card.java): rarity 未指定時は rarityOrDefault() で COMMON。
- [CardDescriber.java](../../../src/main/java/core/presentation/render/CardDescriber.java): 効果説明、射程・範囲、継続時間の表記。[CardEffect.java](../../../src/main/java/core/domain/card/CardEffect.java): Damage の既定 range=1、areaRadius=0。
- [BuffKindLabels.java](../../../src/main/java/core/presentation/render/BuffKindLabels.java) と [Strings.java](../../../src/main/java/core/presentation/render/Strings.java): 物攻／魔攻／物防／魔防／速度の対応。
- [CardRenderer.java](../../../src/main/java/core/presentation/render/CardRenderer.java): drawCardComposite の約 108×72 のアート領域。コードはアートテクスチャ全体を指定幅・高さへ描画するため、切り出し・比率調整を前処理として提案。
- 各行の実装説明は上記ソースの規則を読み取り、監査スクリプトで再現した参考文字列。Java を起動して得た出力ではない。JSON の effect 自体も併記している。
- 元画像 60 枚と当初に読んだ JSON・表示実装の計 68 ファイルについて SHA-256 を監査開始時・終了時で照合し、すべて一致した。追加で読んだ CardEffect.java・BuffKindLabels.java・CardRarity.java は読み取りのみ。
- 作成物は指定 workers 配下の本報告、元データ・目視転記の JSON、一時拡大 PNG、報告生成・検証スクリプトのみ。リポジトリおよび main コピーへの変更、Git の書き込み、Gradle、GUI 起動、ネットワーク、有料 API は使用していない。
