package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.SteamGameData;

import java.util.List;

/**
 * @param type Steam 앱 타입 ("game", "dlc", "music" 등)
 * @param data    type이 "game"일 때만 채워진다. developers는 비어 있으며 normalize 단계에서 채운다.
 * @param english 영어 텍스트. 수집 때 영어 응답을 못 받았으면 null
 * @param languages 지원 언어 (게임이 아니면 없음)
 */
public record ParsedSteamApp(
        String type,
        SteamGameData data,
        List<String> developerNames,
        List<String> publisherNames,
        EnglishText english,
        SteamLanguages languages
) {

    public record EnglishText(String title, String shortDescription, String releaseDateText) {
    }

    public boolean isGame() {
        return SteamAppDetailsParser.TYPE_GAME.equals(type);
    }
}
