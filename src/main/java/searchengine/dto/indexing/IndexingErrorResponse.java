package searchengine.dto.indexing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.dto.Response;


@Data
@EqualsAndHashCode(callSuper = true)
public class IndexingErrorResponse extends Response {

    private String error;
}