package searchengine.services.indexing;

import searchengine.dto.Response;


public interface IndexingService {

    Response startIndexing();

    Response stopIndexing();

    Response indexPage(String url);
}