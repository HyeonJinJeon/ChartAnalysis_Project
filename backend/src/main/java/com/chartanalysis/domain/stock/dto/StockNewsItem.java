package com.chartanalysis.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockNewsItem {
    private final String headline;
    private final String summary;
    private final String url;
    private final String source;
    private final String publishedAt;
}
