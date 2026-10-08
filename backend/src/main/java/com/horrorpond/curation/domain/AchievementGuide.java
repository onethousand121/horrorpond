package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

/**
 * 업적 하나와 달성 방법. 설명과 공략 영상은 선택이다.
 */
@Getter
@Entity
@Table(name = "achievement_guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class AchievementGuide extends BaseTimeEntity {

    public static final int MAX_PER_GAME = 100;
    public static final int MAX_NAME_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(length = MAX_DESCRIPTION_LENGTH)
    private String description;

    /** 공략 영상. 없으면 null */
    @Column(length = YoutubeVideoId.LENGTH)
    private String youtubeId;

    @Column(nullable = false)
    private int sortOrder;

    /**
     * @param videoUrl 유튜브 주소 또는 영상 ID. 비어 있으면 영상 없음
     */
    public static AchievementGuide create(Long gameId, String name, String description, String videoUrl,
                                          int sortOrder) {
        AchievementGuide guide = new AchievementGuide();
        guide.gameId = Objects.requireNonNull(gameId, "gameId");
        guide.name = PlayVideo.optionalText(name, MAX_NAME_LENGTH, "AchievementGuide.name");
        if (guide.name == null) {
            throw new DomainValidationException("AchievementGuide.name must not be blank");
        }
        guide.description = PlayVideo.optionalText(description, MAX_DESCRIPTION_LENGTH,
                "AchievementGuide.description");
        guide.youtubeId = videoUrl == null || videoUrl.isBlank() ? null : YoutubeVideoId.parse(videoUrl);
        guide.sortOrder = sortOrder;
        return guide;
    }
}
