package backend.repository;

import backend.entity.VisitorSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorSessionRepository extends JpaRepository<VisitorSession, Long> {

    Optional<VisitorSession> findBySessionId(String sessionId);

    @Query("SELECT COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate AND v.firstVisit <= :endDate")
    Long countVisitorsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(v) FROM VisitorSession v WHERE v.lastActivity >= :since")
    Long countActiveVisitors(@Param("since") LocalDateTime since);

    @Query("SELECT v.country, COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate GROUP BY v.country")
    List<Object[]> getVisitorsByCountry(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT v.deviceType, COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate GROUP BY v.deviceType")
    List<Object[]> getVisitorsByDeviceType(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT v.browser, COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate GROUP BY v.browser")
    List<Object[]> getVisitorsByBrowser(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT DATE(v.firstVisit), COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate GROUP BY DATE(v.firstVisit)")
    List<Object[]> getDailyVisitors(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT HOUR(v.firstVisit), COUNT(v) FROM VisitorSession v WHERE v.firstVisit >= :startDate GROUP BY HOUR(v.firstVisit)")
    List<Object[]> getHourlyVisitors(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(v.pageViews) FROM VisitorSession v WHERE v.firstVisit >= :startDate")
    Double getAveragePageViews(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(v.totalDuration) FROM VisitorSession v WHERE v.firstVisit >= :startDate AND v.totalDuration > 0")
    Double getAverageSessionDuration(@Param("startDate") LocalDateTime startDate);
}
