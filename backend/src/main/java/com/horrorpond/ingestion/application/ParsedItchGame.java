package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.SteamGameData;

import java.time.LocalDate;
import java.util.List;

/**
 * itch.io 게임 페이지에서 읽은 값. 작성자는 이름만 있고, 개발사 엔티티는 저장할 때 찾거나 만든다.
 */
public record ParsedItchGame(
        long itchId,
        String title,
        String description,
        String coverUrl,
        Integer ratingCount,
        LocalDate releaseDate,
        List<String> tags,
        List<String> languages,
        List<String> authors,
        List<SteamGameData.Media> screenshots
) {
}
