package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 번역 1건을 1트랜잭션으로 반영한다.
 */
@Component
@RequiredArgsConstructor
public class TranslationWriter {

    private final GameRepository gameRepository;

    /**
     * @return 반영했으면 true. 번역하는 사이 소개가 바뀌었거나 게임이 없으면 false
     */
    @Transactional
    public boolean apply(long gameId, String sourceText, String translated) {
        return gameRepository.findById(gameId)
                .map(game -> game.applyKoreanTranslation(sourceText, translated))
                .orElse(false);
    }
}
