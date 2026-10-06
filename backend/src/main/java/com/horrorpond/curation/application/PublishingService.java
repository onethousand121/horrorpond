package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.CurationArticle;
import com.horrorpond.curation.repository.CurationArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 글과 게임을 한 트랜잭션에서 함께 공개한다. 글 검증(highlights 1개 이상 등)에 실패하면 둘 다 공개되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PublishingService {

    private final GameRepository gameRepository;
    private final CurationArticleRepository articleRepository;
    private final Clock clock;

    @Transactional
    public AdminGameResponse publish(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
        CurationArticle article = articleRepository.findByGameId(gameId)
                .orElseThrow(() -> new DomainStateException(
                        "A curation article is required to publish: gameId=" + gameId));
        Instant now = clock.instant();
        article.publish(now);
        game.publish(article.isPublished(), now);
        return AdminGameResponse.of(game, article);
    }
}
