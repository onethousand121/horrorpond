package com.horrorpond.curation.api;

import com.horrorpond.common.web.PageResponse;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.application.GameDetailResponse;
import com.horrorpond.curation.application.GameSummaryResponse;
import com.horrorpond.curation.application.PublicGameQueryService;
import com.horrorpond.curation.repository.CuratedGameSort;
import com.horrorpond.curation.repository.ReleaseWindow;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class PublicGameController {

    static final int MAX_PAGE_SIZE = 48;
    static final int MAX_SEARCH_LENGTH = 100;

    private final PublicGameQueryService queryService;

    @GetMapping
    public PageResponse<GameSummaryResponse> list(
            @RequestParam(required = false) @Size(max = MAX_SEARCH_LENGTH) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean coop,
            @RequestParam(required = false) ReleaseWindow release,
            @RequestParam(defaultValue = "false") boolean picked,
            @RequestParam(defaultValue = "false") boolean korean,
            @RequestParam(defaultValue = "LATEST") CuratedGameSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "ko") @Pattern(regexp = Language.PARAM_PATTERN) String lang) {
        return PageResponse.from(queryService.list(q, genre, coop, release, picked, korean, sort, Language.from(lang),
                PageRequest.of(page, size)));
    }

    @GetMapping("/{slug}")
    public GameDetailResponse detail(
            @PathVariable String slug,
            @RequestParam(defaultValue = "ko") @Pattern(regexp = Language.PARAM_PATTERN) String lang) {
        return queryService.detail(slug, Language.from(lang));
    }
}
