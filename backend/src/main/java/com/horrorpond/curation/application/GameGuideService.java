package com.horrorpond.curation.application;

import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.domain.AchievementGuide;
import com.horrorpond.curation.domain.PlayVideo;
import com.horrorpond.curation.repository.AchievementGuideRepository;
import com.horrorpond.curation.repository.PlayVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임별 플레이 영상과 업적 공략. 관리 화면은 목록 전체를 한 번에 저장한다(순서 = 목록 순서).
 * 빈 목록을 저장하면 모두 지워지고 사이트에서도 해당 영역이 사라진다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GameGuideService {

    private final GameRepository gameRepository;
    private final PlayVideoRepository playVideoRepository;
    private final AchievementGuideRepository achievementGuideRepository;

    @Transactional(readOnly = true)
    public List<PlayVideoResponse> playVideos(Long gameId) {
        return playVideoRepository.findByGameIdOrderBySortOrderAsc(gameId).stream()
                .map(PlayVideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementGuideResponse> achievements(Long gameId) {
        return achievementGuideRepository.findByGameIdOrderBySortOrderAsc(gameId).stream()
                .map(AchievementGuideResponse::from)
                .toList();
    }

    public List<PlayVideoResponse> replacePlayVideos(Long gameId, List<PlayVideoCommand> commands) {
        requireGame(gameId);
        requireAtMost(commands, PlayVideo.MAX_PER_GAME, "play videos");
        // 검증을 먼저 끝내고(잘못된 주소면 기존 목록을 지우지 않는다) 교체한다
        List<PlayVideo> videos = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            videos.add(PlayVideo.create(gameId, commands.get(i).url(), commands.get(i).title(), i));
        }
        playVideoRepository.deleteByGameId(gameId);
        return playVideoRepository.saveAll(videos).stream().map(PlayVideoResponse::from).toList();
    }

    public List<AchievementGuideResponse> replaceAchievements(Long gameId, List<AchievementCommand> commands) {
        requireGame(gameId);
        requireAtMost(commands, AchievementGuide.MAX_PER_GAME, "achievements");
        List<AchievementGuide> guides = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            AchievementCommand command = commands.get(i);
            guides.add(AchievementGuide.create(gameId, command.name(), command.description(), command.videoUrl(), i));
        }
        achievementGuideRepository.deleteByGameId(gameId);
        return achievementGuideRepository.saveAll(guides).stream().map(AchievementGuideResponse::from).toList();
    }

    private void requireGame(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new NotFoundException("Game not found: " + gameId);
        }
    }

    private static void requireAtMost(List<?> items, int max, String what) {
        if (items.size() > max) {
            throw new DomainValidationException("At most " + max + " " + what + " per game");
        }
    }

    /**
     * @param url   유튜브 주소 또는 영상 ID
     * @param title 비어 있으면 제목 없음
     */
    public record PlayVideoCommand(String url, String title) {
    }

    /**
     * @param videoUrl 공략 영상 주소. 비어 있으면 영상 없음
     */
    public record AchievementCommand(String name, String description, String videoUrl) {
    }
}
