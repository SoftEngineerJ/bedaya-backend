package backend.service;

import backend.entity.VisitorSession;
import backend.entity.PageVisit;
import backend.repository.VisitorSessionRepository;
import backend.repository.PageVisitRepository;
import backend.model.PageVisitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsService {

    @Autowired
    private VisitorSessionRepository visitorSessionRepository;

    @Autowired
    private PageVisitRepository pageVisitRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public void trackPageVisit(PageVisitRequest request) {
        // Get or create session
        VisitorSession session = visitorSessionRepository.findBySessionId(request.getSessionId())
                .orElseGet(() -> createNewSession(request));

        // Update session
        updateSession(session, request);

        // Create page visit
        PageVisit pageVisit = createPageVisit(session, request);

        // Save entities
        visitorSessionRepository.save(session);
        pageVisitRepository.save(pageVisit);
    }

    private VisitorSession createNewSession(PageVisitRequest request) {
        VisitorSession session = new VisitorSession();
        session.setSessionId(request.getSessionId());
        session.setIpAddress(request.getIpAddress());
        session.setUserAgent(request.getUserAgent());
        session.setLanguage(request.getLanguage());
        session.setReferrer(request.getReferrer());
        session.setFirstVisit(LocalDateTime.ofEpochSecond(request.getTimestamp() / 1000, 0, ZoneOffset.UTC));
        session.setLastActivity(session.getFirstVisit());
        session.setPageViews(0);
        session.setTotalDuration(0L);

        // Parse device and browser
        parseUserAgent(request.getUserAgent(), session);

        // Get location from IP (simplified - in production use proper IP geolocation
        // service)
        parseLocationFromIp(request.getIpAddress(), session);

        return session;
    }

    private void updateSession(VisitorSession session, PageVisitRequest request) {
        session.setLastActivity(LocalDateTime.ofEpochSecond(request.getTimestamp() / 1000, 0, ZoneOffset.UTC));
        session.setPageViews(session.getPageViews() + 1);
        session.setTotalDuration(session.getTotalDuration() + request.getTimeOnPage());
    }

    private PageVisit createPageVisit(VisitorSession session, PageVisitRequest request) {
        PageVisit pageVisit = new PageVisit();
        pageVisit.setSession(session);
        pageVisit.setPageUrl(request.getPageUrl());
        pageVisit.setPageTitle(request.getPageTitle());
        pageVisit.setTimestamp(LocalDateTime.ofEpochSecond(request.getTimestamp() / 1000, 0, ZoneOffset.UTC));
        pageVisit.setTimeOnPage(request.getTimeOnPage());
        pageVisit.setReferrer(request.getReferrer());
        pageVisit.setIsExitPage(request.getIsExitPage());
        pageVisit.setIsBounce(session.getPageViews() == 1);

        return pageVisit;
    }

    private void parseUserAgent(String userAgent, VisitorSession session) {
        String ua = userAgent.toLowerCase();

        // Device type detection
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
            if (ua.contains("ipad") || (ua.contains("android") && ua.contains("mobile") == false)) {
                session.setDeviceType(VisitorSession.DeviceType.TABLET);
            } else {
                session.setDeviceType(VisitorSession.DeviceType.MOBILE);
            }
        } else {
            session.setDeviceType(VisitorSession.DeviceType.DESKTOP);
        }

        // Browser detection
        if (ua.contains("chrome")) {
            session.setBrowser(VisitorSession.Browser.CHROME);
        } else if (ua.contains("firefox")) {
            session.setBrowser(VisitorSession.Browser.FIREFOX);
        } else if (ua.contains("safari")) {
            session.setBrowser(VisitorSession.Browser.SAFARI);
        } else if (ua.contains("edge")) {
            session.setBrowser(VisitorSession.Browser.EDGE);
        } else if (ua.contains("opera")) {
            session.setBrowser(VisitorSession.Browser.OPERA);
        } else {
            session.setBrowser(VisitorSession.Browser.UNKNOWN);
        }
    }

    private void parseLocationFromIp(String ipAddress, VisitorSession session) {
        // Simplified location detection - in production use proper IP geolocation
        // service
        // For now, just set some default values or use a simple mapping
        if (ipAddress.startsWith("127.") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.")) {
            session.setCountry("Germany");
            session.setCity("Berlin");
        } else {
            // In production, integrate with MaxMind GeoIP2 or similar service
            session.setCountry("Unknown");
            session.setCity("Unknown");
        }
    }

    // Statistics methods
    public Map<String, Object> getOverallStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.minusDays(30);

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalVisitors", visitorSessionRepository.count());
        stats.put("todayVisitors", visitorSessionRepository.countVisitorsByDateRange(todayStart, now));
        stats.put("yesterdayVisitors", visitorSessionRepository.countVisitorsByDateRange(yesterdayStart, todayStart));
        stats.put("weekVisitors", visitorSessionRepository.countVisitorsByDateRange(weekStart, now));
        stats.put("monthVisitors", visitorSessionRepository.countVisitorsByDateRange(monthStart, now));
        stats.put("activeVisitors", visitorSessionRepository.countActiveVisitors(now.minusMinutes(30)));

        stats.put("totalPageViews", pageVisitRepository.count());
        stats.put("todayPageViews",
                pageVisitRepository.getDailyPageViews(todayStart).stream().mapToLong(obj -> (Long) obj[1]).sum());

        stats.put("averagePageViews", visitorSessionRepository.getAveragePageViews(monthStart));
        stats.put("averageSessionDuration", visitorSessionRepository.getAverageSessionDuration(monthStart));

        return stats;
    }

    public Map<String, Long> getVisitorsByCountry() {
        LocalDateTime weekStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(7);
        List<Object[]> results = visitorSessionRepository.getVisitorsByCountry(weekStart);

        return results.stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1]));
    }

    public Map<String, Long> getVisitorsByDeviceType() {
        LocalDateTime weekStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(7);
        List<Object[]> results = visitorSessionRepository.getVisitorsByDeviceType(weekStart);

        return results.stream()
                .collect(Collectors.toMap(
                        obj -> ((VisitorSession.DeviceType) obj[0]).name(),
                        obj -> (Long) obj[1]));
    }

    public Map<String, Long> getVisitorsByBrowser() {
        LocalDateTime weekStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(7);
        List<Object[]> results = visitorSessionRepository.getVisitorsByBrowser(weekStart);

        return results.stream()
                .collect(Collectors.toMap(
                        obj -> ((VisitorSession.Browser) obj[0]).name(),
                        obj -> (Long) obj[1]));
    }

    public Map<String, Long> getDailyVisitors() {
        LocalDateTime monthStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(30);
        List<Object[]> results = visitorSessionRepository.getDailyVisitors(monthStart);

        return results.stream()
                .collect(Collectors.toMap(
                        obj -> obj[0].toString(),
                        obj -> (Long) obj[1]));
    }

    public Map<String, Long> getHourlyVisitors() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<Object[]> results = visitorSessionRepository.getHourlyVisitors(todayStart);

        return results.stream()
                .collect(Collectors.toMap(
                        obj -> obj[0] + ":00",
                        obj -> (Long) obj[1]));
    }

    public List<Map<String, Object>> getMostVisitedPages() {
        LocalDateTime weekStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(7);
        List<Object[]> results = pageVisitRepository.getMostVisitedPages(weekStart);

        return results.stream()
                .map(obj -> {
                    Map<String, Object> page = new HashMap<>();
                    page.put("url", obj[0]);
                    page.put("views", obj[1]);
                    return page;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTopReferrers() {
        LocalDateTime weekStart = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(7);
        List<Object[]> results = pageVisitRepository.getTopReferrers(weekStart);

        return results.stream()
                .map(obj -> {
                    Map<String, Object> referrer = new HashMap<>();
                    referrer.put("source", obj[0]);
                    referrer.put("visits", obj[1]);
                    return referrer;
                })
                .collect(Collectors.toList());
    }

    public String getOverallStatsAsJson() {
        try {
            Map<String, Object> stats = getOverallStats();
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize stats\"}";
        }
    }
}
