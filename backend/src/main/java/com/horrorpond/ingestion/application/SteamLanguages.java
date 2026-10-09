package com.horrorpond.ingestion.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * appdetails의 supported_languages를 언어 코드로 바꾼다.
 * 예: "영어<strong>*</strong>, 한국어<br><strong>*</strong>음성이 지원되는 언어" → languages [en, ko], audio [en]
 * 한국어 응답(l=koreana) 이름을 쓰고, 지역화가 안 된 응답을 위해 영어 이름도 받는다. 모르는 이름은 버린다.
 */
record SteamLanguages(List<String> languages, List<String> audioLanguages) {

    static final SteamLanguages NONE = new SteamLanguages(List.of(), List.of());

    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("한국어", "ko"), Map.entry("Korean", "ko"),
            Map.entry("영어", "en"), Map.entry("English", "en"),
            Map.entry("일본어", "ja"), Map.entry("Japanese", "ja"),
            Map.entry("중국어 간체", "zh-Hans"), Map.entry("Simplified Chinese", "zh-Hans"),
            Map.entry("중국어 번체", "zh-Hant"), Map.entry("Traditional Chinese", "zh-Hant"),
            Map.entry("프랑스어", "fr"), Map.entry("French", "fr"),
            Map.entry("독일어", "de"), Map.entry("German", "de"),
            Map.entry("이탈리아어", "it"), Map.entry("Italian", "it"),
            Map.entry("스페인어 - 스페인", "es"), Map.entry("Spanish - Spain", "es"),
            Map.entry("스페인어 - 중남미", "es-419"), Map.entry("Spanish - Latin America", "es-419"),
            Map.entry("포르투갈어 - 브라질", "pt-BR"), Map.entry("Portuguese - Brazil", "pt-BR"),
            Map.entry("포르투갈어 - 포르투갈", "pt-PT"), Map.entry("Portuguese - Portugal", "pt-PT"),
            Map.entry("러시아어", "ru"), Map.entry("Russian", "ru"),
            Map.entry("폴란드어", "pl"), Map.entry("Polish", "pl"),
            Map.entry("튀르키예어", "tr"), Map.entry("터키어", "tr"), Map.entry("Turkish", "tr"),
            Map.entry("우크라이나어", "uk"), Map.entry("Ukrainian", "uk"),
            Map.entry("태국어", "th"), Map.entry("Thai", "th"),
            Map.entry("베트남어", "vi"), Map.entry("Vietnamese", "vi"),
            Map.entry("인도네시아어", "id"), Map.entry("Indonesian", "id"),
            Map.entry("아랍어", "ar"), Map.entry("Arabic", "ar"),
            Map.entry("체코어", "cs"), Map.entry("Czech", "cs"),
            Map.entry("덴마크어", "da"), Map.entry("Danish", "da"),
            Map.entry("네덜란드어", "nl"), Map.entry("Dutch", "nl"),
            Map.entry("핀란드어", "fi"), Map.entry("Finnish", "fi"),
            Map.entry("그리스어", "el"), Map.entry("Greek", "el"),
            Map.entry("헝가리어", "hu"), Map.entry("Hungarian", "hu"),
            Map.entry("노르웨이어", "no"), Map.entry("Norwegian", "no"),
            Map.entry("루마니아어", "ro"), Map.entry("Romanian", "ro"),
            Map.entry("스웨덴어", "sv"), Map.entry("Swedish", "sv"),
            Map.entry("불가리아어", "bg"), Map.entry("Bulgarian", "bg"));

    static SteamLanguages parse(String supportedLanguages) {
        if (supportedLanguages == null || supportedLanguages.isBlank()) {
            return NONE;
        }
        // "<br>" 뒤는 "*음성이 지원되는 언어" 같은 설명이다
        String list = supportedLanguages.split("(?i)<br\\s*/?>", 2)[0];
        Set<String> languages = new LinkedHashSet<>();
        Set<String> audio = new LinkedHashSet<>();
        for (String item : list.split(",")) {
            boolean withAudio = item.contains("*");
            String name = TAGS.matcher(item).replaceAll("").replace("*", "").strip();
            String code = CODES.get(name);
            if (code == null) {
                continue;
            }
            languages.add(code);
            if (withAudio) {
                audio.add(code);
            }
        }
        return new SteamLanguages(new ArrayList<>(languages), new ArrayList<>(audio));
    }
}
