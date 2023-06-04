package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

public record StatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
}
