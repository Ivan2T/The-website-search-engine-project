package searchengine.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.dto.Response;

import java.util.ArrayList;
import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResponse extends Response {

    private int count;

    private String error;

    private List<SearchData> data = new ArrayList<>();
}