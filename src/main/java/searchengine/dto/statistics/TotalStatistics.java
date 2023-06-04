package searchengine.dto.statistics;

import lombok.Data;


public record TotalStatistics(int sites, int pages, int lemmas, boolean indexing) {
}
