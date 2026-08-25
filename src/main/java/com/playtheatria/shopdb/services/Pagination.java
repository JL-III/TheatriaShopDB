package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.web.ApiException;

import java.util.Collections;
import java.util.List;

public final class Pagination {
    public static <T> List<T> getPage(List<T> sourceList, int page, int pageSize) {
        if (pageSize <= 0 || page <= 0) {
            throw new ApiException(400, "invalid page size: " + pageSize);
        }

        int fromIndex = (page - 1) * pageSize;
        if (sourceList == null || sourceList.size() <= fromIndex) {
            return Collections.emptyList();
        }

        // toIndex exclusive
        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    public static Integer getNumPages(int pageSize, long totalResults) {
        if (pageSize == 0 || totalResults == 0) return 0;
        return (int) Math.ceil(totalResults / (double) pageSize);
    }
}
