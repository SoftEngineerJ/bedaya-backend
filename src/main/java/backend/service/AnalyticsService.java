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

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Duration GEO_CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration GEO_REQUEST_TIMEOUT = Duration.ofMillis(800);
    private static final HttpClient GEO_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(GEO_CONNECT_TIMEOUT)
            .build();
    private final Map<String, GeoResult> geoCache = new ConcurrentHashMap<>();

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
        GeoResult geo = resolveGeoFromIp(ipAddress);
        session.setCountry(geo.country());
        session.setCity(geo.city());
    }

    private GeoResult resolveGeoFromIp(String ipAddress) {
        String ip = ipAddress == null ? "" : ipAddress.trim();
        if (ip.isBlank()) {
            return GeoResult.UNKNOWN;
        }

        if (!isLikelyIpAddress(ip)) {
            return GeoResult.UNKNOWN;
        }

        if (isPrivateOrLocalIp(ip)) {
            return GeoResult.UNKNOWN;
        }

        GeoResult cached = geoCache.get(ip);
        if (cached != null) {
            return cached;
        }

        GeoResult resolved = fetchGeoFromIpApi(ip);
        geoCache.put(ip, resolved);
        return resolved;
    }

    private boolean isPrivateOrLocalIp(String ip) {
        return ip.startsWith("127.")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.20.")
                || ip.startsWith("172.21.")
                || ip.startsWith("172.22.")
                || ip.startsWith("172.23.")
                || ip.startsWith("172.24.")
                || ip.startsWith("172.25.")
                || ip.startsWith("172.26.")
                || ip.startsWith("172.27.")
                || ip.startsWith("172.28.")
                || ip.startsWith("172.29.")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || ip.equalsIgnoreCase("localhost")
                || ip.equals("::1");
    }

    private GeoResult fetchGeoFromIpApi(String ip) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipwho.is/" + ip))
                    .header("accept", "application/json")
                    .timeout(GEO_REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = GEO_HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return GeoResult.UNKNOWN;
            }

            Map<?, ?> json = objectMapper.readValue(response.body(), Map.class);
            Object success = json.get("success");
            if (success instanceof Boolean && !((Boolean) success)) {
                return GeoResult.UNKNOWN;
            }

            Object countryName = json.get("country");
            Object city = json.get("city");

            String countryStr = countryName == null ? "Unknown" : countryName.toString().trim();
            String cityStr = city == null ? "Unknown" : city.toString().trim();

            if (countryStr.isBlank()) {
                countryStr = "Unknown";
            }
            if (cityStr.isBlank()) {
                cityStr = "Unknown";
            }

            return new GeoResult(countryStr, cityStr);
        } catch (Exception e) {
            return GeoResult.UNKNOWN;
        }
    }

    private boolean isLikelyIpAddress(String value) {
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private record GeoResult(String country, String city) {
        private static final GeoResult UNKNOWN = new GeoResult("Unknown", "Unknown");
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
