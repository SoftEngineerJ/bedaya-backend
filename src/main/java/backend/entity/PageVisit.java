package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "page_visits")
public class PageVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private VisitorSession session;

    @Column(nullable = false)
    private String pageUrl;

    @Column
    private String pageTitle;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Long timeOnPage = 0L; // in seconds

    @Column
    private String referrer;

    @Column
    private Boolean isExitPage = false;

    @Column
    private Boolean isBounce = false;
}
