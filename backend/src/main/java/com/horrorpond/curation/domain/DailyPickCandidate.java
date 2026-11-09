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

import java.time.LocalDate;
import java.util.Objects;

/**
 * "오늘의 게임" 후보. 큐레이터가 쓴 한 줄 소개와 함께 메인에 하루씩 돌아가며 보인다.
 * 개발사에게 키를 받은 게임은 sponsored(개발사 제공 표시), 특정 날짜에 보이게 하려면 featuredOn.
 */
@Getter
@Entity
@Table(name = "daily_pick_candidate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class DailyPickCandidate extends BaseTimeEntity {

    public static final int MAX_INTRO_LENGTH = 120;
    public static final int MAX_INTRO_EN_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long gameId;

    @Column(nullable = false, length = MAX_INTRO_LENGTH)
    private String intro;

    /** 영어 화면용 소개 (선택). 없으면 영어 화면도 한국어 */
    @Column(length = MAX_INTRO_EN_LENGTH)
    private String introEn;

    /** 개발사에게 키·자료를 제공받은 게임 */
    @Column(nullable = false)
    private boolean sponsored;

    /** 이 날짜(한국)에 고정. 없으면 순서대로 돌아간다 */
    private LocalDate featuredOn;

    public static DailyPickCandidate create(Long gameId, String intro, String introEn, boolean sponsored,
                                            LocalDate featuredOn) {
        DailyPickCandidate candidate = new DailyPickCandidate();
        candidate.gameId = Objects.requireNonNull(gameId, "gameId");
        candidate.change(intro, introEn, sponsored, featuredOn);
        return candidate;
    }

    public void change(String intro, String introEn, boolean sponsored, LocalDate featuredOn) {
        String text = PlayVideo.optionalText(intro, MAX_INTRO_LENGTH, "DailyPickCandidate.intro");
        if (text == null) {
            throw new DomainValidationException("DailyPickCandidate.intro must not be blank");
        }
        this.intro = text;
        this.introEn = PlayVideo.optionalText(introEn, MAX_INTRO_EN_LENGTH, "DailyPickCandidate.introEn");
        this.sponsored = sponsored;
        this.featuredOn = featuredOn;
    }

    /** 순서대로 돌아가는 후보인지: 날짜 고정이 없거나 이미 지난 날짜 (앞으로의 날짜는 그날을 위해 아껴 둔다) */
    public boolean rotatesOn(LocalDate today) {
        return featuredOn == null || featuredOn.isBefore(today);
    }
}
