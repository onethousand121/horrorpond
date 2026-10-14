package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.catalog.domain.Game;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.common.error.NotFoundException;
import com.horrorpond.curation.application.GameGuideResponses.AchievementGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.EndingGuideResponse;
import com.horrorpond.curation.application.GameGuideResponses.EndingsResponse;
import com.horrorpond.curation.application.GameGuideResponses.KoreanPatchResponse;
import com.horrorpond.curation.application.GameGuideResponses.PlayVideoResponse;
import com.horrorpond.curation.domain.AchievementGuide;
import com.horrorpond.curation.domain.EndingGuide;
import com.horrorpond.curation.domain.KoreanPatch;
import com.horrorpond.curation.domain.PlayVideo;
import com.horrorpond.curation.domain.VideoKind;
import com.horrorpond.curation.repository.AchievementGuideRepository;
import com.horrorpond.curation.repository.EndingGuideRepository;
import com.horrorpond.curation.repository.KoreanPatchRepository;
import com.horrorpond.curation.repository.PlayVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임별 플레이 영상·업적 공략·엔딩 공략·유저 한글패치. 관리 화면은 목록 전체를 한 번에 저장한다(순서 = 목록 순서).
 * 빈 목록을 저장하면 모두 지워지고 사이트에서도 해당 영역이 사라진다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GameGuideService {

    private final GameRepository gameRepository;
    private final PlayVideoRepository playVideoRepository;
    private final AchievementGuideRepository achievementGuideRepository;
    private final EndingGuideRepository endingGuideRepository;
    private final KoreanPatchRepository koreanPatchRepository;

    /** 주인장 플레이 영상 */
    @Transactional(readOnly = true)
    public List<PlayVideoResponse> playVideos(Long gameId) {
        return videos(gameId, VideoKind.KEEPER);
    }

    /** 다른 채널의 참고 영상 */
    @Transactional(readOnly = true)
    public List<PlayVideoResponse> referenceVideos(Long gameId) {
        return videos(gameId, VideoKind.REFERENCE);
    }

    private List<PlayVideoResponse> videos(Long gameId, VideoKind kind) {
        return playVideoRepository.findByGameIdAndKindOrderBySortOrderAsc(gameId, kind).stream()
                .map(PlayVideoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementGuideResponse> achievements(Long gameId) {
        return achievementGuideRepository.findByGameIdOrderBySortOrderAsc(gameId).stream()
                .map(AchievementGuideResponse::from)
                .toList();
    }

    /** 엔딩 목록 (멀티 엔딩일 때만 있다) */
    @Transactional(readOnly = true)
    public List<EndingGuideResponse> endings(Long gameId) {
        return endingGuideRepository.findByGameIdOrderBySortOrderAsc(gameId).stream()
                .map(EndingGuideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KoreanPatchResponse> koreanPatches(Long gameId) {
        return koreanPatchRepository.findByGameIdOrderBySortOrderAsc(gameId).stream()
                .map(KoreanPatchResponse::from)
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
        playVideoRepository.deleteByGameIdAndKind(gameId, VideoKind.KEEPER);
        return playVideoRepository.saveAll(videos).stream().map(PlayVideoResponse::from).toList();
    }

    /**
     * 참고 영상 목록 전체를 바꾼다. 주인장 영상은 건드리지 않는다.
     */
    public List<PlayVideoResponse> replaceReferenceVideos(Long gameId, List<ReferenceVideoCommand> commands) {
        requireGame(gameId);
        requireAtMost(commands, PlayVideo.MAX_PER_GAME, "reference videos");
        List<PlayVideo> videos = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            ReferenceVideoCommand command = commands.get(i);
            videos.add(PlayVideo.reference(gameId, command.url(), command.title(), command.channelName(), i));
        }
        playVideoRepository.deleteByGameIdAndKind(gameId, VideoKind.REFERENCE);
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

    /**
     * 엔딩 구분과 엔딩 목록을 함께 바꾼다. 엔딩 목록은 멀티 엔딩일 때만 둘 수 있다
     * (단일·표시 안 함에 목록을 보내면 400, 빈 목록이면 기존 목록을 지운다).
     */
    public EndingsResponse replaceEndings(Long gameId, EndingType endingType, List<EndingCommand> commands) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
        if (endingType != EndingType.MULTIPLE && !commands.isEmpty()) {
            throw new DomainValidationException("Endings are allowed only for MULTIPLE ending games");
        }
        requireAtMost(commands, EndingGuide.MAX_PER_GAME, "endings");
        List<EndingGuide> endings = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            EndingCommand command = commands.get(i);
            endings.add(EndingGuide.create(gameId, command.name(), command.description(), command.videoUrl(), i));
        }
        // 구분을 먼저 바꾼다: 아래 삭제 쿼리가 영속성 컨텍스트를 flush 후 비우므로 그 전에 반영돼야 한다
        game.changeEndingType(endingType);
        endingGuideRepository.deleteByGameId(gameId);
        List<EndingGuideResponse> saved = endingGuideRepository.saveAll(endings).stream()
                .map(EndingGuideResponse::from)
                .toList();
        return new EndingsResponse(endingType, saved);
    }

    public List<KoreanPatchResponse> replaceKoreanPatches(Long gameId, List<KoreanPatchCommand> commands) {
        requireGame(gameId);
        requireAtMost(commands, KoreanPatch.MAX_PER_GAME, "korean patches");
        List<KoreanPatch> patches = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            KoreanPatchCommand command = commands.get(i);
            patches.add(KoreanPatch.create(gameId, command.url(), command.author(), command.note(), i));
        }
        koreanPatchRepository.deleteByGameId(gameId);
        return koreanPatchRepository.saveAll(patches).stream().map(KoreanPatchResponse::from).toList();
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
     * @param channelName 영상을 올린 채널 이름 (필수, 사이트에 출처로 보인다)
     */
    public record ReferenceVideoCommand(String url, String title, String channelName) {
    }

    /**
     * @param videoUrl 공략 영상 주소. 비어 있으면 영상 없음
     */
    public record AchievementCommand(String name, String description, String videoUrl) {
    }

    /**
     * @param description 엔딩 보는 방법. 비어 있으면 없음
     * @param videoUrl    엔딩 영상 주소(쇼츠 포함). 비어 있으면 영상 없음
     */
    public record EndingCommand(String name, String description, String videoUrl) {
    }

    /**
     * @param url    배포처 주소 (https만)
     * @param author 제작자. 비어 있으면 없음
     * @param note   짧은 메모. 비어 있으면 없음
     */
    public record KoreanPatchCommand(String url, String author, String note) {
    }
}
