package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.client.SteamTransientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 게임 1개의 appdetails를 스냅샷에 넣을 모양으로 받는다: 한국어 appdetails + (없으면) appreviews 리뷰 수 + 영어 텍스트.
 * 하루 1회 enrichment와 매시간 출시 확인({@link ReleaseCheckService})이 함께 쓴다. 트랜잭션 밖에서 부른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SteamAppDetailsFetcher {

    private final SteamStoreClient storeClient;

    /**
     * @throws SteamRateLimitedException 429/403 (부른 쪽이 기다렸다가 다시 시도하거나 중단한다)
     * @throws SteamTransientException   appdetails가 재시도 후에도 실패
     */
    public AppDetailsResult fetch(int appid) {
        return withEnglish(appid, withReviewCount(appid, storeClient.fetchAppDetails(appid)));
    }

    /**
     * 자동 노출 기준이 리뷰 수라, appdetails에 리뷰 수가 빠진 게임은 appreviews로 한 번 더 받는다.
     * 실패하면 리뷰 수 없이 저장하고 다음 갱신 때 다시 받는다 (429는 호출한 쪽의 대기/재시도로 넘긴다).
     */
    private AppDetailsResult withReviewCount(int appid, AppDetailsResult result) {
        if (!(result instanceof AppDetailsResult.Found found) || found.hasReviewCount()) {
            return result;
        }
        try {
            Integer reviewCount = storeClient.fetchReviewCount(appid);
            return reviewCount == null ? found : found.withReviewCount(reviewCount);
        } catch (SteamTransientException e) {
            log.warn("appreviews failed after retries: appid={}, {}", appid, e.getMessage());
            return found;
        }
    }

    /**
     * 영어 사이트용 텍스트. 실패하면 영어 없이 저장하고(정규화 때 이전 영어 값을 유지) 다음 갱신 때 다시 받는다.
     */
    private AppDetailsResult withEnglish(int appid, AppDetailsResult result) {
        if (!(result instanceof AppDetailsResult.Found found)) {
            return result;
        }
        try {
            String english = storeClient.fetchEnglishText(appid);
            return english == null ? found : found.withEnglish(english);
        } catch (SteamTransientException e) {
            log.warn("appdetails(english) failed after retries: appid={}, {}", appid, e.getMessage());
            return found;
        }
    }
}
