package com.summit.ddd.application.vo;

import java.util.List;

/**
 * 统一的游标分页载体
 * @param <T> 记录类型
 */
public record CursorResult<T>(List<T> records, String nextCursor, boolean hasMore) {

    /** 空页：无记录、无游标、无更多。 */
    public static <T> CursorResult<T> empty() {
        return new CursorResult<>(List.of(), null, false);
    }
}
