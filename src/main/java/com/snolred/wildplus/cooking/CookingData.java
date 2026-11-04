package com.snolred.wildplus.cooking;

/**
 * 요리 2차 레시피 표 (tools/cooking_gen.py 가 만든 파일 — 직접 고치지 말 것).
 * 재료: "minecraft:…" · "wildplus:…" · "@fish"(물고기 아무거나). 그릇: bowl / bottle / 없음.
 */
public final class CookingData {
    private CookingData() {}

    public enum Station {
        POT("가마솥", 12, true), OVEN("화덕 오븐", 15, true), GRILL("바비큐 그릴", 8, true),
        STOVE("화구", 6, true), PREP("도마 작업대", 3, false), AGING("숙성통 선반", 120, false);

        public final String ko;
        public final int seconds;
        public final boolean fuel;

        Station(String ko, int seconds, boolean fuel) {
            this.ko = ko;
            this.seconds = seconds;
            this.fuel = fuel;
        }
    }

    public record Eff(String id, int seconds, int amp) {}

    public record Dish(String id, String ko, Station station, String[] ingredients, int count, String container, Eff[] effects,
                       int nutrition, float saturation) {}

    public static final Dish[] DISHES = {
        new Dish("kimchi_stew", "김치찌개", Station.POT, new String[]{"wildplus:kimchi", "minecraft:porkchop", "wildplus:onion", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("strength", 180, 0)}, 8, 0.7F),
        new Dish("spicy_fish_stew", "매운탕", Station.POT, new String[]{"@fish", "@fish", "wildplus:chili", "wildplus:onion", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("strength", 180, 0), new Eff("resistance", 120, 0)}, 9, 0.8F),
        new Dish("seafood_stew", "해물탕", Station.POT, new String[]{"minecraft:cod", "minecraft:salmon", "minecraft:kelp", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("water_breathing", 300, 0), new Eff("dolphins_grace", 120, 0)}, 8, 0.8F),
        new Dish("curry_rice", "카레라이스", Station.POT, new String[]{"wildplus:rice", "minecraft:potato", "minecraft:carrot", "wildplus:onion", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("haste", 240, 0)}, 9, 0.7F),
        new Dish("ginseng_chicken", "삼계탕", Station.POT, new String[]{"minecraft:chicken", "wildplus:ginseng", "wildplus:rice", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("regeneration", 30, 1), new Eff("health_boost", 300, 1)}, 8, 0.8F),
        new Dish("seaweed_soup", "미역국", Station.POT, new String[]{"minecraft:kelp", "minecraft:kelp", "minecraft:beef", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("regeneration", 45, 0)}, 8, 0.7F),
        new Dish("mushroom_cream_soup", "버섯 크림 수프", Station.POT, new String[]{"minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:wheat", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("absorption", 120, 0)}, 8, 0.7F),
        new Dish("onion_soup", "양파 수프", Station.POT, new String[]{"wildplus:onion", "wildplus:onion", "minecraft:bread", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("fire_resistance", 300, 0)}, 8, 0.7F),
        new Dish("cheese_pizza", "치즈 피자", Station.OVEN, new String[]{"minecraft:wheat", "minecraft:wheat", "wildplus:tomato", "wildplus:cheese"}, 1, "", new Eff[]{new Eff("absorption", 120, 1)}, 8, 0.7F),
        new Dish("apple_pie", "애플파이", Station.OVEN, new String[]{"minecraft:apple", "minecraft:apple", "minecraft:wheat", "minecraft:sugar", "minecraft:egg"}, 1, "", new Eff[]{new Eff("speed", 180, 0)}, 9, 0.7F),
        new Dish("strawberry_cake", "딸기 케이크", Station.OVEN, new String[]{"minecraft:wheat", "minecraft:wheat", "wildplus:strawberry", "wildplus:strawberry", "minecraft:sugar", "minecraft:egg"}, 1, "", new Eff[]{new Eff("absorption", 180, 1), new Eff("regeneration", 20, 0)}, 10, 0.8F),
        new Dish("grape_tart", "포도 타르트", Station.OVEN, new String[]{"wildplus:grape", "wildplus:grape", "minecraft:wheat", "minecraft:sugar"}, 1, "", new Eff[]{new Eff("jump_boost", 180, 1)}, 8, 0.7F),
        new Dish("gratin", "그라탕", Station.OVEN, new String[]{"minecraft:potato", "minecraft:potato", "wildplus:cheese", "wildplus:onion"}, 1, "", new Eff[]{new Eff("resistance", 180, 0)}, 8, 0.7F),
        new Dish("baguette", "바게트", Station.OVEN, new String[]{"minecraft:wheat", "minecraft:wheat", "minecraft:wheat"}, 2, "", new Eff[]{}, 7, 0.6F),
        new Dish("fish_bread", "붕어빵", Station.OVEN, new String[]{"minecraft:wheat", "minecraft:sugar", "minecraft:sweet_berries", "minecraft:egg"}, 2, "", new Eff[]{new Eff("speed", 120, 0)}, 8, 0.7F),
        new Dish("fish_pie", "생선 파이", Station.OVEN, new String[]{"@fish", "minecraft:wheat", "minecraft:wheat", "minecraft:egg"}, 1, "", new Eff[]{new Eff("water_breathing", 300, 0), new Eff("luck", 180, 0)}, 8, 0.8F),
        new Dish("steak", "스테이크", Station.GRILL, new String[]{"minecraft:beef", "minecraft:beef", "wildplus:onion"}, 1, "", new Eff[]{new Eff("health_boost", 300, 0), new Eff("strength", 120, 0)}, 7, 0.8F),
        new Dish("bulgogi", "불고기", Station.GRILL, new String[]{"minecraft:beef", "wildplus:onion", "minecraft:sugar", "minecraft:carrot"}, 1, "", new Eff[]{new Eff("strength", 240, 0)}, 8, 0.7F),
        new Dish("pork_belly", "삼겹살", Station.GRILL, new String[]{"minecraft:porkchop", "minecraft:porkchop", "wildplus:cabbage", "wildplus:onion"}, 1, "", new Eff[]{new Eff("strength", 120, 0), new Eff("regeneration", 20, 0)}, 8, 0.8F),
        new Dish("grilled_fish_set", "생선구이 정식", Station.GRILL, new String[]{"@fish", "wildplus:rice", "wildplus:cabbage"}, 1, "", new Eff[]{new Eff("luck", 300, 0)}, 7, 0.7F),
        new Dish("chicken_skewer", "닭꼬치", Station.GRILL, new String[]{"minecraft:chicken", "minecraft:stick", "wildplus:chili"}, 2, "", new Eff[]{new Eff("speed", 180, 0)}, 7, 0.7F),
        new Dish("lamb_chop", "양갈비", Station.GRILL, new String[]{"minecraft:mutton", "minecraft:mutton", "wildplus:onion"}, 1, "", new Eff[]{new Eff("resistance", 180, 0)}, 7, 0.7F),
        new Dish("dragon_king_grill", "용왕 구이", Station.GRILL, new String[]{"wildplus:dragon_king_fish", "wildplus:golden_carp", "wildplus:ginseng"}, 1, "", new Eff[]{new Eff("regeneration", 120, 1), new Eff("strength", 300, 1), new Eff("resistance", 300, 1)}, 7, 0.9F),
        new Dish("fried_rice", "볶음밥", Station.STOVE, new String[]{"wildplus:rice", "minecraft:egg", "minecraft:carrot", "wildplus:onion"}, 1, "", new Eff[]{new Eff("haste", 180, 0)}, 8, 0.7F),
        new Dish("tteokbokki", "떡볶이", Station.STOVE, new String[]{"wildplus:rice", "wildplus:rice", "wildplus:chili", "wildplus:chili", "wildplus:cabbage"}, 1, "", new Eff[]{new Eff("strength", 120, 0), new Eff("speed", 120, 0)}, 9, 0.8F),
        new Dish("seafood_pancake", "해물파전", Station.STOVE, new String[]{"minecraft:wheat", "minecraft:egg", "@fish", "wildplus:onion"}, 1, "", new Eff[]{new Eff("water_breathing", 300, 0), new Eff("night_vision", 300, 0)}, 8, 0.8F),
        new Dish("omurice", "오므라이스", Station.STOVE, new String[]{"wildplus:rice", "minecraft:egg", "minecraft:egg", "wildplus:tomato"}, 1, "", new Eff[]{new Eff("health_boost", 240, 0)}, 8, 0.7F),
        new Dish("cream_pasta", "크림 파스타", Station.STOVE, new String[]{"minecraft:wheat", "minecraft:wheat", "wildplus:cheese", "minecraft:brown_mushroom"}, 1, "", new Eff[]{new Eff("regeneration", 30, 0), new Eff("absorption", 120, 0)}, 8, 0.8F),
        new Dish("eel_rice_bowl", "장어덮밥", Station.STOVE, new String[]{"wildplus:storm_eel", "wildplus:rice", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("speed", 120, 1), new Eff("jump_boost", 120, 0)}, 7, 0.8F),
        new Dish("rolled_omelette", "계란말이", Station.STOVE, new String[]{"minecraft:egg", "minecraft:egg", "minecraft:egg", "wildplus:onion"}, 1, "", new Eff[]{new Eff("haste", 120, 0)}, 8, 0.7F),
        new Dish("gimbap", "김밥", Station.PREP, new String[]{"wildplus:rice", "wildplus:rice", "minecraft:dried_kelp", "minecraft:carrot", "minecraft:egg"}, 2, "", new Eff[]{new Eff("speed", 240, 0)}, 9, 0.7F),
        new Dish("sushi_set", "초밥 세트", Station.PREP, new String[]{"@fish", "@fish", "wildplus:rice", "wildplus:rice"}, 1, "", new Eff[]{new Eff("luck", 300, 0), new Eff("water_breathing", 180, 0)}, 8, 0.8F),
        new Dish("hoe_rice_bowl", "회덮밥", Station.PREP, new String[]{"@fish", "wildplus:rice", "wildplus:cabbage", "wildplus:chili", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("haste", 180, 0), new Eff("luck", 180, 0)}, 9, 0.8F),
        new Dish("bibimbap", "비빔밥", Station.PREP, new String[]{"wildplus:rice", "minecraft:carrot", "wildplus:cabbage", "minecraft:egg", "wildplus:chili", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("haste", 240, 0), new Eff("regeneration", 30, 0)}, 10, 0.8F),
        new Dish("fruit_salad", "과일 샐러드", Station.PREP, new String[]{"minecraft:apple", "wildplus:strawberry", "wildplus:grape", "wildplus:blueberry", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("regeneration", 40, 0)}, 9, 0.7F),
        new Dish("kimchi", "김치", Station.PREP, new String[]{"wildplus:cabbage", "wildplus:cabbage", "wildplus:chili", "wildplus:onion"}, 2, "", new Eff[]{}, 8, 0.6F),
        new Dish("cheese", "치즈", Station.AGING, new String[]{"minecraft:milk_bucket"}, 3, "", new Eff[]{}, 5, 0.6F),
        new Dish("wine", "와인", Station.AGING, new String[]{"wildplus:grape", "wildplus:grape", "wildplus:grape", "wildplus:grape", "minecraft:glass_bottle"}, 1, "bottle", new Eff[]{new Eff("resistance", 300, 0), new Eff("strength", 300, 0)}, 9, 0.8F),
        new Dish("beer", "맥주", Station.AGING, new String[]{"minecraft:wheat", "minecraft:wheat", "minecraft:wheat", "minecraft:glass_bottle"}, 1, "bottle", new Eff[]{new Eff("haste", 180, 1)}, 8, 0.7F),
        new Dish("makgeolli", "막걸리", Station.AGING, new String[]{"wildplus:rice", "wildplus:rice", "wildplus:rice", "minecraft:glass_bottle"}, 1, "bottle", new Eff[]{new Eff("regeneration", 60, 0), new Eff("absorption", 300, 0)}, 8, 0.8F),
        new Dish("ginseng_wine", "인삼주", Station.AGING, new String[]{"wildplus:ginseng", "minecraft:sugar", "minecraft:glass_bottle"}, 1, "bottle", new Eff[]{new Eff("health_boost", 600, 1), new Eff("regeneration", 60, 0)}, 7, 0.8F),
        new Dish("butter_cookie", "버터 쿠키", Station.OVEN, new String[]{"minecraft:wheat", "wildplus:butter", "minecraft:sugar", "minecraft:egg"}, 4, "", new Eff[]{new Eff("speed", 120, 0)}, 8, 0.7F),
        new Dish("cheesecake", "치즈케이크", Station.OVEN, new String[]{"wildplus:cheese", "wildplus:cream", "minecraft:sugar", "minecraft:egg", "minecraft:wheat"}, 1, "", new Eff[]{new Eff("absorption", 240, 1), new Eff("regeneration", 20, 0)}, 9, 0.8F),
        new Dish("custard_pudding", "커스터드 푸딩", Station.STOVE, new String[]{"wildplus:milk_bottle", "minecraft:egg", "minecraft:sugar"}, 2, "", new Eff[]{new Eff("jump_boost", 180, 0)}, 7, 0.7F),
        new Dish("carbonara", "까르보나라", Station.STOVE, new String[]{"minecraft:wheat", "minecraft:wheat", "wildplus:cream", "minecraft:egg", "minecraft:porkchop"}, 1, "", new Eff[]{new Eff("strength", 240, 0), new Eff("absorption", 120, 0)}, 9, 0.8F),
        new Dish("cream_stew", "크림 스튜", Station.POT, new String[]{"wildplus:cream", "minecraft:chicken", "minecraft:potato", "minecraft:carrot", "minecraft:bowl"}, 1, "bowl", new Eff[]{new Eff("resistance", 240, 0), new Eff("regeneration", 30, 0)}, 9, 0.8F),
        new Dish("egg_toast", "에그 토스트", Station.STOVE, new String[]{"minecraft:bread", "minecraft:egg", "wildplus:butter", "wildplus:cheese"}, 1, "", new Eff[]{new Eff("haste", 240, 0)}, 8, 0.7F)
    };
}
