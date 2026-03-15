package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "visitor_sessions")
public class VisitorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column
    private String country;

    @Column
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Browser browser;

    @Column
    private String language;

    @Column(nullable = false)
    private LocalDateTime firstVisit;

    @Column(nullable = false)
    private LocalDateTime lastActivity;

    @Column(nullable = false)
    private Integer pageViews = 0;

    @Column
    private Long totalDuration = 0L; // in seconds

    @Column
    private String referrer;

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET, UNKNOWN
    }

    public enum Browser {
        CHROME, FIREFOX, SAFARI, EDGE, OPERA, UNKNOWN
    }
}
