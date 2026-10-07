package com.horrorpond.catalog.domain;

import com.horrorpond.common.domain.DomainValidationException;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Steam에서 가져와 정규화한 게임 데이터. ingestion이 만들어 {@link Game#applySteamData}로 넘긴다.
 * {@link Credit#developer()}는 ingestion이 DeveloperRepository로 조회하거나 저장해 둔 엔티티여야 한다.
 */
public record SteamGameData(
        String title,
        String shortDescription,
        String headerImageUrl,
        LocalDate releaseDate,
        String releaseDateText,
        boolean comingSoon,
        boolean coop,
        /** Steam 리뷰 수 (appdetails recommendations.total). 없으면 null */
        Integer reviewCount,
        /** 성인 콘텐츠 (content_descriptors 3: Adult Only Sexual Content, 4: Frequent Nudity or Sexual Content) */
        boolean adult,
        List<Media> media,
        List<Credit> developers
) {

    public SteamGameData {
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("SteamGameData.title must not be blank");
        }
        media = media == null ? List.of() : List.copyOf(media);
        developers = developers == null ? List.of() : List.copyOf(new LinkedHashSet<>(developers));
    }

    /**
     * ingestion이 개발사 엔티티를 조회/생성한 뒤 크레딧을 채워 넣을 때 쓴다.
     */
    public SteamGameData withDevelopers(List<Credit> credits) {
        return new SteamGameData(title, shortDescription, headerImageUrl, releaseDate, releaseDateText,
                comingSoon, coop, reviewCount, adult, media, credits);
    }

    public record Media(MediaType type, String url, String thumbnailUrl) {

        public Media {
            Objects.requireNonNull(type, "type");
            if (url == null || url.isBlank()) {
                throw new DomainValidationException("SteamGameData.Media.url must not be blank");
            }
        }
    }

    public record Credit(Developer developer, DeveloperRole role) {

        public Credit {
            Objects.requireNonNull(developer, "developer");
            Objects.requireNonNull(role, "role");
        }
    }
}
