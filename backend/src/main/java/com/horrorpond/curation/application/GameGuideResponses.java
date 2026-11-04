package com.horrorpond.curation.application;

import com.horrorpond.catalog.domain.EndingType;
import com.horrorpond.common.domain.Language;
import com.horrorpond.curation.domain.AchievementGuide;
import com.horrorpond.curation.domain.EndingGuide;
import com.horrorpond.curation.domain.KoreanPatch;
import com.horrorpond.curation.domain.PlayVideo;

import java.util.List;

/**
 * 플레이 영상·업적 공략·엔딩·한글패치 응답. 공개 상세와 관리자 상세가 함께 쓴다.
 */
public final class GameGuideResponses {

    private GameGuideResponses() {
    }

    /**
     * @param title       없으면 null
     * @param channelName 참고 영상의 채널 이름. 주인장 영상은 null
     */
    public record PlayVideoResponse(String youtubeId, String title, String channelName) {

        static PlayVideoResponse from(PlayVideo video) {
            return new PlayVideoResponse(video.getYoutubeId(), video.getTitle(), video.getChannelName());
        }
    }

    /**
     * @param description 없으면 null
     * @param youtubeId   공략 영상. 없으면 null
     * @param nameEn      영어 번역 (관리 화면용). 공개 응답에서는 언어에 맞춰 name에 반영하고 null
     */
    public record AchievementGuideResponse(String name, String description, String youtubeId, String iconUrl,
                                           String nameEn, String descriptionEn) {

        static AchievementGuideResponse from(AchievementGuide guide) {
            return new AchievementGuideResponse(guide.getName(), guide.getDescription(), guide.getYoutubeId(),
                    guide.getIconUrl(), guide.getNameEn(), guide.getDescriptionEn());
        }

        /** 공개 화면용: 영어 화면이면 번역이 있는 값만 영어로 바꾼다 */
        public AchievementGuideResponse in(Language language) {
            return new AchievementGuideResponse(language.pick(name, nameEn), language.pick(description, descriptionEn),
                    youtubeId, iconUrl, null, null);
        }
    }

    /**
     * @param description 보는 방법. 없으면 null
     * @param youtubeId   엔딩 영상. 없으면 null
     */
    public record EndingGuideResponse(String name, String description, String youtubeId, String nameEn,
                                      String descriptionEn) {

        static EndingGuideResponse from(EndingGuide ending) {
            return new EndingGuideResponse(ending.getName(), ending.getDescription(), ending.getYoutubeId(),
                    ending.getNameEn(), ending.getDescriptionEn());
        }

        /** 공개 화면용: 영어 화면이면 번역이 있는 값만 영어로 바꾼다 */
        public EndingGuideResponse in(Language language) {
            return new EndingGuideResponse(language.pick(name, nameEn), language.pick(description, descriptionEn),
                    youtubeId, null, null);
        }
    }

    /**
     * @param endingType null이면 표시 안 함
     * @param endings    멀티 엔딩은 여러 개, 단일 엔딩은 최대 1개(스포일러 설명), 표시 안 함은 빈 목록
     */
    public record EndingsResponse(EndingType endingType, List<EndingGuideResponse> endings) {
    }

    /**
     * @param author 없으면 null
     * @param note   없으면 null
     */
    public record KoreanPatchResponse(String url, String author, String note) {

        static KoreanPatchResponse from(KoreanPatch patch) {
            return new KoreanPatchResponse(patch.getUrl(), patch.getAuthor(), patch.getNote());
        }
    }
}
