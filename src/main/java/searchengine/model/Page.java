package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "page")
//@Table(name = "page", indexes = {@Index(columnList = "path", name = "path_index")})
@Setter
@Getter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne (optional=false, cascade=CascadeType.ALL)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex(site_id, path(512))")
    private String path;

    @Column(nullable = false, columnDefinition = "INT")
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    public Page(){
    }

}
