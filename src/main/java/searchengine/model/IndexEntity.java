package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;


@Getter
@Setter
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "index_e", schema = "search_engine")
public class IndexEntity {

    @Id
    @Column(name = "index_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int indexId;

    @NotNull
    @Column(name = "page_id")
    private int pageId;

    @NotNull
    @Column(name = "lemma_id")
    private int lemmaId;

    @NotNull
    @Column(name = "rank_index", columnDefinition = "FLOAT")
    private double rank;

    @ManyToOne
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private Page pageByPageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private Lemma lemmaByLemmaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntity indexEntity = (IndexEntity) o;
        return indexId == indexEntity.indexId && pageId == indexEntity.pageId && lemmaId == indexEntity.lemmaId && Double.compare(indexEntity.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexId, pageId, lemmaId, rank);
    }

    @Override
    public String toString() {
        return "Index{" +
                "indexId=" + indexId +
                ", pageId=" + pageId +
                ", lemmaId=" + lemmaId +
                ", rank=" + rank +
                '}';
    }
}