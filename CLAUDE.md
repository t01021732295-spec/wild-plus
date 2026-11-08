# Wild Plus (야생 플러스) — 작업 안내

마인크래프트 Java **26.3** + **NeoForge 26.3.0.99** 모드. 패키지 `com.snolred.wildplus`.
사용자(스놀레드)와는 **항상 한국어**로 대화. 긴 코드는 파일로. 큰 기능은 **구현 전에 3D 도안(이미지)부터** 보여 주고 승인받기.
친구 넷이 금·토에 주로 플레이하는 서버용.

## 빌드 · 배포
- 로컬에 마인크래프트/Gradle 환경이 없을 수 있음 → **GitHub Actions(CI)** 로 빌드 확인.
  - `.github/workflows/build.yml`: 빌드 → 서버 스모크 테스트 → 클라이언트 실행 후 스크린샷(`shot-*.png`)
  - 결과는 `ci-log` 브랜치(status.txt · build-errors.txt · server-tail.txt · client-filtered.txt · shot-*.png)
  - jar 는 릴리스 `latest` 에 `wildplus-<버전>.jar` 로 올라감
- 버전: `gradle.properties` 의 `mod_version` (최근 3.9.2)
- 새 push 는 진행 중인 CI 를 취소함. 문서만 바꿀 땐 커밋 메시지에 `[skip ci]`.
- API 확인용 `probe.yml`: `.github/probe-classes.txt`(javap 클래스) · `probe-grep.txt` · `probe-javap.txt`(정규식) 를 고치고
  `gh api -X POST repos/t01021732295-spec/wild-plus/actions/workflows/probe.yml/dispatches -f ref=main` → 결과는 `ci-probe` 브랜치 `probe.txt`
  - 26.x 는 이름이 많이 바뀜 (예: `ServerLevel.getStructureTemplateManager()`, `Player.drop` 없음 → `Containers.dropItemStack`,
    `Blocks.WHITE_WOOL`/`RED_BED` 없음, `PoseStack.rotate(...)`, 침대는 `AbstractBedBlock`/`BedRule`). 모르면 probe 로 확인.

## 데이터 생성 (파이썬)
- `cd tools && python3 gen_data.py` → 모델 · 블록상태 · 언어(ko_kr/en_us) · 레시피 · 루트테이블 · 태그 · 구조물 생성
  - `data_v2 … data_v30` 를 차례로 부름. 새 기능은 **아직 없는 번호**의 `data_vNN.py` 를 추가하고 gen_data 끝에 연결 (ModItemsNN · ModBlocksNN 도 마찬가지 — 먼저 ls 로 확인).
  - 텍스처: `gen_textures.py` (`textures_v*`)
- **주의:** 구조물 재생성은 결과가 매번 조금 달라짐 → 생성 후 바꾸려던 구조물만 남기고 나머지는
  `git checkout src/main/resources/data/wildplus/structure/<이름>.nbt` 로 되돌리기.
- 3D 블록 디자인: `kitchen_design.py`(조리기구) · `restaurant_design.py`(식당) · `furniture_design.py`(가구) · `house_design.py`(집 외형 도안)
  → `kitchen_models.export(이름, 도안, lat0, lat1, dep0, dep1, 높이)` 가 모델 JSON + 512 아틀라스 텍스처로 변환
  (도안 좌표: x 오른쪽, y 위, +z 정면, 16 = 1칸, 모델 범위 -16..32 → 32 넘으면 위쪽을 눌러 담음)
- 집 구조물(실제 블록): `house_structures.py` → `data/wildplus/structure/house/*.nbt`
- 도안 미리보기: `cosmetic_design.render()` / `sheet()` 로 PNG 를 그려 사용자에게 보여 줌

## 주요 시스템 (코드 위치)
- 경제: `economy/` (WildData 통장, Tax 누진세, ShipPrices 출하가, Grade 등급, Jobs 일일 알바, Honor 명예, Luxury 명품관)
- 광석 채굴기: `block/OreMinerBlockEntity` (광물별 초, 3.3 에서 1.5배 느리게 · 루비 216초 · 오프라인 보충 없음)
- 요리: `cooking/` + `block/CookingStation*` + `client/CookingScreen` (레시피 표는 `tools/cooking_gen.py` 가 생성)
- 식당: `block/RestaurantCounter*` · `entity/CustomerEntity` · `menu/RestaurantMenu` · `client/RestaurantScreen` · 명령 `/restaurant name|review|reply`
- 집 꾸미기(3.3): `block/FurnitureBlock`(Kind 별 기능) · `FurnitureBedBlock` · `entity/SeatEntity` · `home/HouseKitItem` · `home/HomeShop` · `client/HousePreview`
- 여러 칸 블록: `block/BigBlock`(본체가 모델 전체를 그림) + `BigPartBlock`(보이지 않는 부속 칸, 클릭/부수기 전달)
- 화면 공통: `client/UiKit` · `UiButton` · `menu/PickMenu`(목록/상점 진열장 화면)
  · 3.9 시설별 테마 화면 `client/ThemedScreen` — 서버가 `PickMenu.themed(PickMenu.T_…)` 를 flags 에 넣으면 그 모양으로 (약국 · 기상 · 땅 · 워프 · 대회 · 순간이동기 · 계약서 · 감옥 · 제단)
- 땅(부동산): `content/Land` — 사면 모서리에 `block/LandPostBlock`(주인 색 8가지) 자동 · 내 땅 들어서면 큰 제목 · 감옥 포로 그림은 `entity/PrisonerEntity`(저장 안 함, `Prisoners.sync`)
- 물가(3.7): `economy/Prices.x()` — 모든 값을 3배(100만동 이상은 2배)로. 새 가격도 이걸로 감싸기
- 흉조(3.8): `omen/` — `OmenRaids`(바닐라 습격 + 계약서 저주 · 테마 · 연쇄 · 기여도 보상) · `OmenEvents`(투척병 · 양조 세기 · /omen)
  · `Prisoners`(올가미 → 감옥: 몸값/전향/심문) · `CounterRaid`(전초 기지 역습격 · 점령) · `Smugglers`(흉조 때만 오는 상인) · `OmenAltar`(마을 수호 게이지)
  · `Discounts`(마을의 영웅 할인을 모드 상인 · 서비스 · 마을 자판기에) / 몹 `entity/MercenaryEntity` · `SmugglerEntity` (변명자 모델 + `client/IllagerPropsLayer` 소품)
  · 스킨/아이콘은 `tools/illager_skins.py` 가 바닐라 그림(WILDPLUS_VANILLA_REF 폴더)에서 색만 바꿔 만듦 · 후반 이벤트는 `RandomEvents` 의 `late()`

## 커밋
커밋 메시지 끝에:
```
Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```
