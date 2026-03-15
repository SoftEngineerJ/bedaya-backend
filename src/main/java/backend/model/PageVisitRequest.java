package backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageVisitRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Page URL is required")
    private String pageUrl;

    private String pageTitle;

    @NotBlank(message = "User agent is required")
    private String userAgent;

    private String ipAddress;

    private String referrer;

    private String language;

    private Long timeOnPage = 0L;

    private Boolean isExitPage = false;

    @NotNull(message = "Timestamp is required")
    private Long timestamp;
}
