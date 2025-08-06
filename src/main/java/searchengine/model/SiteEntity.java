package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "site", schema = "search_engine")
public class SiteEntity {

    @Id
    @Column(name = "site_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int siteId;

    @NotNull
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    @Column(name = "status_time")
    private Timestamp statusTime;

    @Basic
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "siteEBySiteId", cascade = CascadeType.ALL)
    private List<Lemma> lemmaBySiteId = new ArrayList<>();

    @OneToMany(mappedBy = "siteEBySiteId", cascade = CascadeType.ALL)
    private List<Page> pageBySiteId = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteEntity siteEntity = (SiteEntity) o;
        return siteId == siteEntity.siteId && status == siteEntity.status
                && statusTime.equals(siteEntity.statusTime) && url.equals(siteEntity.url) && name.equals(siteEntity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, status, statusTime, url, name);
    }
}