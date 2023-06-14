package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@Setter
@Getter
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id")
    private SiteEntity siteEntity;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String path;

    @Column(nullable = false, columnDefinition = "INT")
    private int code;

    @Column(nullable = false, length = 16777215,
            columnDefinition = "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    public PageEntity() {
    }

}
