package searchengine.model;

import com.sun.istack.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "lemma", schema = "search_engine")
@SQLInsert(sql = "insert into search_engine.lemma(frequency, lemma, site_id) values (?, ?, ?) on duplicate key update frequency = lemma.frequency + 1")
public class Lemma {

    @Id
    @Column(name = "lemma_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int lemmaId;

    @OrderColumn()
    @Column(name = "site_id")
    private int siteId;

    @NotNull
    @Column(name = "lemma")
    private String lemma;

    @Column(name = "frequency")
    private int frequency;

    @OneToMany(mappedBy = "lemmaByLemmaId", cascade = CascadeType.ALL)
    private List<IndexEntity> indexEntityByLemmaId = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private SiteEntity siteEBySiteId;

    public Lemma(int siteId, @NonNull String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return lemmaId == lemma1.lemmaId && siteId == lemma1.siteId && frequency == lemma1.frequency && lemma.equals(lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemmaId, siteId, lemma, frequency);
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "lemmaId=" + lemmaId +
                ", siteId=" + siteId +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}