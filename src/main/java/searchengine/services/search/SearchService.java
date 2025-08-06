package searchengine.services.search;

import searchengine.dto.Response;


public interface SearchService {

    Response search(String query, String site, int offset, int limit);
}