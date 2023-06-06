package searchengine.services;

import searchengine.dto.search.SearchResponse;

import java.io.IOException;

public interface SearchService {
    SearchResponse search(String query, String site, Integer offset, Integer limit) throws IOException;
}
