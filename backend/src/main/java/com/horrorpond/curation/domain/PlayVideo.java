package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.BaseTimeEntity;
import com.horrorpond.common.domain.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
 * 게임의 플레이 영상(유튜브). 게임마다 순서대로 여러 개를 둘 수 있다.
 */
@Getter
@Entity
@Table(name = "play_video")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class PlayVideo extends BaseTimeEntity {

    public static final int MAX_PER_GAME = 10;
    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_CHANNEL_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, length = YoutubeVideoId.LENGTH)
    private String youtubeId;

    /** 없으면 null (사이트는 "플레이 영상 N"으로 보여준다) */
    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoKind kind = VideoKind.KEEPER;

    /** 참고 영상의 채널 이름 (주인장 영상은 null) */
    @Column(length = MAX_CHANNEL_LENGTH)
    private String channelName;

    /**
     * @param url 유튜브 주소 또는 영상 ID
     */
    public static PlayVideo create(Long gameId, String url, String title, int sortOrder) {
        PlayVideo video = new PlayVideo();
        video.gameId = Objects.requireNonNull(gameId, "gameId");
        video.youtubeId = YoutubeVideoId.parse(url);
        video.title = optionalText(title, MAX_TITLE_LENGTH, "PlayVideo.title");
        video.sortOrder = sortOrder;
        return video;
    }

    /**
     * 다른 채널의 참고 영상. 사이트가 출처로 보여주므로 채널 이름이 있어야 한다.
     */
    public static PlayVideo reference(Long gameId, String url, String title, String channelName, int sortOrder) {
        PlayVideo video = create(gameId, url, title, sortOrder);
        String channel = optionalText(channelName, MAX_CHANNEL_LENGTH, "PlayVideo.channelName");
        if (channel == null) {
            throw new DomainValidationException("PlayVideo.channelName is required for a reference video");
        }
        video.kind = VideoKind.REFERENCE;
        video.channelName = channel;
        return video;
    }

    static String optionalText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > maxLength) {
            throw new DomainValidationException(field + " must be at most " + maxLength + " characters");
        }
        return text;
    }
}
