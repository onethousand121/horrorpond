package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.Developer;
import com.horrorpond.catalog.repository.DeveloperRepository;
import com.horrorpond.common.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 개발사(작성자)를 이름으로 찾고, 없으면 만든다. 호출하는 쪽 트랜잭션 안에서 쓴다 (Steam·itch.io 공통).
 */
@Component
@RequiredArgsConstructor
class DeveloperResolver {

    private static final String DEVELOPER_SLUG_FALLBACK = "developer";

    private final DeveloperRepository developerRepository;

    Developer findOrCreate(String name) {
        return developerRepository.findByName(name)
                .orElseGet(() -> developerRepository.save(Developer.create(name,
                        NormalizeItemProcessor.uniqueSlug(SlugGenerator.slugify(name, DEVELOPER_SLUG_FALLBACK),
                                developerRepository::existsBySlug))));
    }
}
