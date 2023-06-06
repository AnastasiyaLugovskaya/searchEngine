package searchengine.dto.search;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchData> data = new ArrayList<>();
    private String error;

    public void dataAdd(SearchData searchData){
        data.add(searchData);
    }
}
