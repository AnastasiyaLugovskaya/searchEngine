package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity(name = "page")
@Setter
@Getter
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne (optional=false, cascade=CascadeType.MERGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id")
    private SiteEntity siteEntity;

    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex(site_id, path(512))")
    private String path;

    @Column(nullable = false, columnDefinition = "INT")
    private int code;

    @Column(nullable = false, length = 16777215,
            columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    public PageEntity(){
    }

}
