package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.CurationArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 큐레이터 공개: 자동 노출 조건(리뷰 수, 성인 여부)과 무관하게 게임을 항상 노출한다.
 * 글은 선택이다. 장점 포인트가 1개 이상인 글이 있으면 함께 공개하고, 없거나 작성 중이면 게임만 공개한다.
 */
@Service
@RequiredArgsConstructor
public class PublishingService {

    private final GameRepository gameRepository;
    private final CurationArticleRepository articleRepository;
    private final ExposurePolicy exposurePolicy;
    private final Clock clock;

    @Transactional
    public AdminGameResponse publish(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
        Instant now = clock.instant();
        CurationArticle article = articleRepository.findByGameId(gameId).orElse(null);
        if (article != null && !article.getHighlights().isEmpty()) {
            article.publish(now);
        }
        game.publish(now);
        return AdminGameResponse.of(game, article, exposurePolicy);
    }
}
