package com.horrorpond.curation.application;

import com.horrorpond.curation.domain.AchievementGuide;
import com.horrorpond.curation.domain.PlayVideo;

/**
 * 플레이 영상·업적 공략 응답. 공개 상세와 관리자 상세가 함께 쓴다.
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
     */
    public record AchievementGuideResponse(String name, String description, String youtubeId) {

        static AchievementGuideResponse from(AchievementGuide guide) {
            return new AchievementGuideResponse(guide.getName(), guide.getDescription(), guide.getYoutubeId());
        }
    }
}
