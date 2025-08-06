package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "page", schema = "search_engine", indexes = @Index(columnList = "path"))
public class Page {

    @Id
    @Column(name = "page_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int pageId;

    @NotNull
    @Column(name = "site_id")
    private int siteId;

    @NotNull
    @Basic(optional = false)
    @Column(name = "path", columnDefinition = "VARCHAR(255) NOT NULL")
    private String path;

    @NotNull
    @Column(name = "code")
    private int code;

    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @NotNull
    @Column(name = "title", columnDefinition = "VARCHAR(255)")
    private String title;

    @ManyToOne
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private SiteEntity siteEBySiteId;

    @OneToMany(mappedBy = "pageByPageId", cascade = CascadeType.ALL)
    private List<IndexEntity> indexEntityByPageId = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return pageId == page.pageId && siteId == page.siteId && code == page.code
                && path.equals(page.path) && content.equals(page.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, siteId, path, code, content);
    }

    @Override
    public String toString() {
        return "Page{" +
                "pageId=" + pageId +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content(trim by 100)='" + content.substring(0, 30) + '\'' +
                '}';
    }
}