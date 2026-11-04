package com.horrorpond.curation.domain;

import com.horrorpond.common.domain.DomainValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * 업적 아이콘 주소. 이미지를 따로 저장하지 않고 Steam 커뮤니티 CDN의 업적 아이콘
 * (https://cdn.*.steamstatic.com/steamcommunity/public/images/apps/{appid}/{파일}) 주소만 받는다.
 */
public final class AchievementIconUrl {

    public static final int MAX_LENGTH = 500;

    private static final Set<String> HOSTS = Set.of(
            "cdn.akamai.steamstatic.com",
            "cdn.fastly.steamstatic.com",
            "cdn.cloudflare.steamstatic.com",
            "steamcdn-a.akamaihd.net");
    private static final String PATH_PREFIX = "/steamcommunity/public/images/apps/";

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
                || !HOSTS.contains(host) || !path.startsWith(PATH_PREFIX) || path.contains("..")) {
            throw invalid(url);
        }
        return url;
    }

    private static DomainValidationException invalid(String url) {
        return new DomainValidationException("AchievementGuide.iconUrl must be a Steam achievement icon URL: " + url);
    }
}
