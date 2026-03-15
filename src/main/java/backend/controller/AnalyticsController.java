package backend.controller;

import backend.model.PageVisitRequest;
import backend.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:3002",
        "https://bedaya-study.vercel.app",
        "https://bedayastudy.vercel.app",
        "https://bedaya-admin.vercel.app"
})
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @PostMapping("/track")
    public ResponseEntity<String> trackPageVisit(@Valid @RequestBody PageVisitRequest request,
            HttpServletRequest httpRequest) {
        try {
            if (!StringUtils.hasText(request.getIpAddress())) {
                String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(forwardedFor)) {
                    request.setIpAddress(forwardedFor.split(",")[0].trim());
                } else {
                    request.setIpAddress(httpRequest.getRemoteAddr());
                }
            }

            if (!StringUtils.hasText(request.getUserAgent())) {
                String ua = httpRequest.getHeader("User-Agent");
                if (StringUtils.hasText(ua)) {
                    request.setUserAgent(ua);
                }
            }

            analyticsService.trackPageVisit(request);
            return ResponseEntity.ok("Page visit tracked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error tracking page visit: " + e.getMessage());
        }
    }

    @GetMapping("/stats/overall")
    public ResponseEntity<Map<String, Object>> getOverallStats() {
        Map<String, Object> stats = analyticsService.getOverallStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/countries")
    public ResponseEntity<Map<String, Long>> getVisitorsByCountry() {
        Map<String, Long> stats = analyticsService.getVisitorsByCountry();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/devices")
    public ResponseEntity<Map<String, Long>> getVisitorsByDeviceType() {
        Map<String, Long> stats = analyticsService.getVisitorsByDeviceType();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/browsers")
    public ResponseEntity<Map<String, Long>> getVisitorsByBrowser() {
        Map<String, Long> stats = analyticsService.getVisitorsByBrowser();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<Map<String, Long>> getDailyVisitors() {
        Map<String, Long> stats = analyticsService.getDailyVisitors();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/hourly")
    public ResponseEntity<Map<String, Long>> getHourlyVisitors() {
        Map<String, Long> stats = analyticsService.getHourlyVisitors();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/pages")
    public ResponseEntity<java.util.List<Map<String, Object>>> getMostVisitedPages() {
        java.util.List<Map<String, Object>> pages = analyticsService.getMostVisitedPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/stats/referrers")
    public ResponseEntity<java.util.List<Map<String, Object>>> getTopReferrers() {
        java.util.List<Map<String, Object>> referrers = analyticsService.getTopReferrers();
        return ResponseEntity.ok(referrers);
    }

    @GetMapping("/stats/live")
    public ResponseEntity<Map<String, Object>> getLiveStats() {
        Map<String, Object> liveStats = analyticsService.getOverallStats();
        return ResponseEntity.ok(liveStats);
    }
}
