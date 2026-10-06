package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.SteamGameData;

import java.util.List;

/**
 * @param type Steam 앱 타입 ("game", "dlc", "music" 등)
 * @param data type이 "game"일 때만 채워진다. developers는 비어 있으며 normalize 단계에서 채운다.
 */
public record ParsedSteamApp(
        String type,
        SteamGameData data,
        List<String> developerNames,
        List<String> publisherNames
) {

    public boolean isGame() {
        return SteamAppDetailsParser.TYPE_GAME.equals(type);
    }
}
