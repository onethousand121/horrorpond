package com.horrorpond.catalog.domain;

import com.horrorpond.common.domain.DomainValidationException;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * itch.io 게임 페이지에서 가져온 데이터. ingestion이 만들어 {@link Game#applyItchData}로 넘긴다.
 * itch.io는 영어 페이지 하나뿐이라 이름·소개는 한 벌이다 (한국어 화면은 자동 번역을 쓴다).
 *
 * @param ratingCount 평가 수. Steam 리뷰 수 자리에 넣어 노출 판정·정렬에 같이 쓴다
 * @param developers  ingestion이 DeveloperRepository로 조회하거나 저장해 둔 작성자
 */
public record ItchGameData(
        String url,
        String title,
        String shortDescription,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        String releaseDateTextEn,
        Integer ratingCount,
        List<String> tags,
        List<String> languages,
        List<SteamGameData.Media> media,
        List<Developer> developers
) {

    public ItchGameData {
        if (url == null || url.isBlank()) {
            throw new DomainValidationException("ItchGameData.url must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("ItchGameData.title must not be blank");
        }
        tags = tags == null ? List.of() : List.copyOf(tags);
        languages = languages == null ? List.of() : List.copyOf(languages);
        media = media == null ? List.of() : List.copyOf(media);
        developers = developers == null ? List.of() : List.copyOf(new LinkedHashSet<>(developers));
    }
}
