package com.horrorpond.curation.api;

import com.horrorpond.curation.application.HubStatsResponse;
import com.horrorpond.curation.application.PublicGameQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 사이트 홈의 숫자 한 줄 (오늘 출시 / 내일 출시 예정).
 * /api/games/{slug}와 겹치지 않게 따로 둔다.
 */
@RestController
@RequiredArgsConstructor
public class HubStatsController {

    private final PublicGameQueryService queryService;

    @GetMapping("/api/stats")
    public HubStatsResponse stats() {
        return queryService.stats();
    }
}
