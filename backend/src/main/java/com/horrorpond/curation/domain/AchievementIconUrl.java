package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * 업적 아이콘 주소. 이미지를 따로 저장하지 않고 Steam 커뮤니티 CDN의 업적 아이콘 주소만 받는다:
 * https://shared.*.steamstatic.com/community_assets/images/apps/{appid}/{파일} (최근 게임은 여기에만 있다)
 * 또는 예전 주소 https://cdn.*.steamstatic.com/steamcommunity/public/images/apps/{appid}/{파일}.
 */
public final class AchievementIconUrl {

    public static final int MAX_LENGTH = 500;

    private static final Set<String> SHARED_HOSTS = Set.of(
            "shared.akamai.steamstatic.com",
            "shared.fastly.steamstatic.com",
            "shared.cloudflare.steamstatic.com");
    private static final String SHARED_PATH_PREFIX = "/community_assets/images/apps/";
    private static final Set<String> LEGACY_HOSTS = Set.of(
            "cdn.akamai.steamstatic.com",
            "cdn.fastly.steamstatic.com",
            "cdn.cloudflare.steamstatic.com",
            "steamcdn-a.akamaihd.net");
    private static final String LEGACY_PATH_PREFIX = "/steamcommunity/public/images/apps/";

    private AchievementIconUrl() {
    }

    /** 비어 있으면 null(아이콘 없음). 허용한 Steam CDN의 업적 이미지가 아니면 DomainValidationException */
    public static String parseOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String url = value.strip();
        if (url.length() > MAX_LENGTH) {
            throw new DomainValidationException("AchievementGuide.iconUrl must be at most " + MAX_LENGTH + " chars");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw invalid(url);
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1
                || !isSteamIcon(host, path) || path.contains("..")) {
            throw invalid(url);
        }
        return url;
    }

    private static boolean isSteamIcon(String host, String path) {
        return SHARED_HOSTS.contains(host) && path.startsWith(SHARED_PATH_PREFIX)
                || LEGACY_HOSTS.contains(host) && path.startsWith(LEGACY_PATH_PREFIX);
    }

    private static DomainValidationException invalid(String url) {
        return new DomainValidationException("AchievementGuide.iconUrl must be a Steam achievement icon URL: " + url);
    }
}
