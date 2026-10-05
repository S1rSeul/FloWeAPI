package com.floweapp.flowe_api.common.dto;

import java.util.List;

public record CursorPageResponseDto<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {}
