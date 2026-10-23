package com.horrorpond.curation.api;

import com.horrorpond.curation.application.PublicGameQueryService;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.SitemapRow;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사이트맵 전용: 공개 게임의 주소만 한 번에 준다 (목록 API를 48개씩 넘기며 부르지 않도록).
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final PublicGameQueryService queryService;

    @GetMapping("/api/sitemap/games")
    public List<SitemapRow> games() {
        return queryService.sitemap();
    }
}
