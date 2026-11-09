package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.domain.QGame;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.domain.Language;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.domain.DailyPickCandidate;
import com.horrorpond.curation.repository.CuratedGameQueryRepository;
import com.horrorpond.curation.repository.CuratedGameQueryRepository.PublicGameQuery;
import com.horrorpond.curation.repository.CuratedGameSort;
import com.horrorpond.curation.repository.DailyPickCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "오늘의 게임". 큐레이터가 고른 후보 중 오늘(한국) 날짜에 고정된 게임, 없으면 후보를 하루씩 돌아가며 고른다.
 * 공개 사이트에 보이지 않는 게임(노출 규칙 미충족·성인)은 건너뛰고, 후보가 하나도 없으면 주인장 추천 게임에서 고른다.
 */
@Service
@RequiredArgsConstructor
public class DailyPickService {

    /** 후보가 없을 때 대신 고를 주인장 추천 게임 수 */
    private static final int FALLBACK_POOL = 100;

    private final DailyPickCandidateRepository candidateRepository;
    private final GameRepository gameRepository;
    private final CuratedGameQueryRepository curatedGameQueryRepository;
    private final PublicGameQueryService publicGameQueryService;
    private final ExposurePolicy exposurePolicy;

    @Transactional(readOnly = true)
    public Optional<DailyPickResponse> today(Language language) {
        LocalDate today = exposurePolicy.today();
        List<DailyPickCandidate> candidates = candidateRepository.findAllByOrderByIdAsc();
        Map<Long, Game> games = gameRepository.findAllById(candidates.stream().map(DailyPickCandidate::getGameId).toList())
                .stream().collect(Collectors.toMap(Game::getId, Function.identity()));
        List<DailyPickCandidate> showable = candidates.stream()
                .filter(c -> isShowable(games.get(c.getGameId())))
                .toList();

        Optional<DailyPickCandidate> chosen = showable.stream()
                .filter(c -> today.equals(c.getFeaturedOn()))
                .findFirst()
                .or(() -> rotate(showable.stream().filter(c -> c.rotatesOn(today)).toList(), today));
        if (chosen.isPresent()) {
            DailyPickCandidate candidate = chosen.get();
            return summary(candidate.getGameId(), language).map(game -> new DailyPickResponse(today,
                    DailyPickResponse.Source.CANDIDATE, language.pick(candidate.getIntro(), candidate.getIntroEn()),
                    candidate.isSponsored(), game));
        }
        return fallbackFromPicks(today, language);
    }

    /** 관리 화면: 모든 후보와 공개 여부 */
    @Transactional(readOnly = true)
    public List<AdminDailyPickResponse> candidates() {
        List<DailyPickCandidate> candidates = candidateRepository.findAllByOrderByIdAsc();
        Map<Long, Game> games = gameRepository.findAllById(candidates.stream().map(DailyPickCandidate::getGameId).toList())
                .stream().collect(Collectors.toMap(Game::getId, Function.identity()));
        return candidates.stream().map(c -> {
            Game game = games.get(c.getGameId());
            return new AdminDailyPickResponse(c.getGameId(), game == null ? null : game.getTitle(),
                    game == null ? null : game.getSlug(), c.getIntro(), c.getIntroEn(), c.isSponsored(),
                    c.getFeaturedOn(), isShowable(game));
        }).toList();
    }

    /**
     * 후보로 저장(없으면 추가, 있으면 수정). 같은 날짜에 다른 게임이 고정돼 있으면 400.
     */
    @Transactional
    public AdminDailyPickResponse save(Long gameId, String intro, String introEn, boolean sponsored,
                                       LocalDate featuredOn) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
        if (featuredOn != null) {
            candidateRepository.findByFeaturedOn(featuredOn)
                    .filter(other -> !other.getGameId().equals(gameId))
                    .ifPresent(other -> {
                        throw new DomainValidationException("Another game is already featured on " + featuredOn);
                    });
        }
        DailyPickCandidate candidate = candidateRepository.findByGameId(gameId)
                .map(existing -> {
                    existing.change(intro, introEn, sponsored, featuredOn);
                    return existing;
                })
                .orElseGet(() -> candidateRepository.save(
                        DailyPickCandidate.create(gameId, intro, introEn, sponsored, featuredOn)));
        return new AdminDailyPickResponse(gameId, game.getTitle(), game.getSlug(), candidate.getIntro(),
                candidate.getIntroEn(), candidate.isSponsored(), candidate.getFeaturedOn(), isShowable(game));
    }

    @Transactional
    public void remove(Long gameId) {
        candidateRepository.findByGameId(gameId).ifPresent(candidateRepository::delete);
    }

    private boolean isShowable(Game game) {
        return game != null && !game.isAdult() && exposurePolicy.isVisible(game);
    }

    /** 날짜마다 다음 후보로 (후보 순서는 등록 순) */
    private static Optional<DailyPickCandidate> rotate(List<DailyPickCandidate> pool, LocalDate today) {
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pool.get(Math.floorMod(today.toEpochDay(), pool.size())));
    }

    private Optional<DailyPickResponse> fallbackFromPicks(LocalDate today, Language language) {
        List<GameSummaryResponse> picks = publicGameQueryService.list(null, null, null, null, true, false, false,
                false, CuratedGameSort.LATEST, language, PageRequest.of(0, FALLBACK_POOL)).getContent().stream()
                .filter(game -> !game.adult())
                .toList();
        if (picks.isEmpty()) {
            return Optional.empty();
        }
        GameSummaryResponse game = picks.get(Math.floorMod(today.toEpochDay(), picks.size()));
        return Optional.of(new DailyPickResponse(today, DailyPickResponse.Source.PICK, game.oneLiner(), false, game));
    }

    private Optional<GameSummaryResponse> summary(Long gameId, Language language) {
        return publicGameQueryService.summary(gameId, language);
    }

    /**
     * @param source CANDIDATE(큐레이터가 고른 후보) | PICK(후보가 없어 주인장 추천 게임에서 고름)
     * @param intro  한 줄 소개 (PICK이면 추천 글의 한 줄 평)
     */
    public record DailyPickResponse(LocalDate date, Source source, String intro, boolean sponsored,
                                    GameSummaryResponse game) {

        public enum Source { CANDIDATE, PICK }
    }

    /**
     * @param showable 지금 공개 사이트에 보일 수 있는지 (아니면 메인에 나오지 않는다: 공개 버튼 필요)
     */
    public record AdminDailyPickResponse(Long gameId, String title, String slug, String intro, String introEn,
                                         boolean sponsored, LocalDate featuredOn, boolean showable) {
    }
}
