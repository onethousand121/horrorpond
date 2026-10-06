package com.horrorpond.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Page를 그대로 직렬화하지 않고(내부 구조가 노출되고 버전마다 바뀜) 필요한 필드만 내보낸다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
