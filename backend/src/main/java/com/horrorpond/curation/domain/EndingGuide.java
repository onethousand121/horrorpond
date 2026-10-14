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
 * 멀티 엔딩 게임의 엔딩 하나와 보는 방법. 설명과 영상은 선택이다.
 * 사이트는 스포일러라 접어 두고, 누르면 보여준다.
 */
@Getter
@Entity
@Table(name = "ending_guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class EndingGuide extends BaseTimeEntity {

    public static final int MAX_PER_GAME = 30;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    /** 엔딩 보는 방법. 없으면 null */
    @Column(columnDefinition = "text")
    private String description;

    /** 엔딩 영상. 없으면 null */
    @Column(length = YoutubeVideoId.LENGTH)
    private String youtubeId;

    @Column(nullable = false)
    private int sortOrder;

    /**
     * @param videoUrl 유튜브 주소(쇼츠 포함) 또는 영상 ID. 비어 있으면 영상 없음
     */
    public static EndingGuide create(Long gameId, String name, String description, String videoUrl, int sortOrder) {
        EndingGuide ending = new EndingGuide();
        ending.gameId = Objects.requireNonNull(gameId, "gameId");
        ending.name = PlayVideo.optionalText(name, MAX_NAME_LENGTH, "EndingGuide.name");
        if (ending.name == null) {
            throw new DomainValidationException("EndingGuide.name must not be blank");
        }
        ending.description = PlayVideo.optionalText(description, MAX_DESCRIPTION_LENGTH, "EndingGuide.description");
        ending.youtubeId = videoUrl == null || videoUrl.isBlank() ? null : YoutubeVideoId.parse(videoUrl);
        ending.sortOrder = sortOrder;
        return ending;
    }
}
