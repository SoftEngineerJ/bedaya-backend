package backend.repository;

import backend.entity.PageVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageVisitRepository extends JpaRepository<PageVisit, Long> {

    @Query("SELECT p.pageUrl, COUNT(p) FROM PageVisit p WHERE p.timestamp >= :startDate GROUP BY p.pageUrl ORDER BY COUNT(p) DESC")
    List<Object[]> getMostVisitedPages(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT p.pageTitle, COUNT(p) FROM PageVisit p WHERE p.timestamp >= :startDate AND p.pageTitle IS NOT NULL GROUP BY p.pageTitle ORDER BY COUNT(p) DESC")
    List<Object[]> getMostVisitedPagesByTitle(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT p.pageUrl, AVG(p.timeOnPage) FROM PageVisit p WHERE p.timestamp >= :startDate AND p.timeOnPage > 0 GROUP BY p.pageUrl ORDER BY AVG(p.timeOnPage) DESC")
    List<Object[]> getAverageTimeOnPage(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT FORMATDATETIME(p.timestamp, 'yyyy-MM-dd'), COUNT(p) FROM PageVisit p WHERE p.timestamp >= :startDate GROUP BY FORMATDATETIME(p.timestamp, 'yyyy-MM-dd') ORDER BY FORMATDATETIME(p.timestamp, 'yyyy-MM-dd')")
    List<Object[]> getDailyPageViews(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT p.pageUrl, COUNT(DISTINCT p.session.sessionId) FROM PageVisit p WHERE p.timestamp >= :startDate GROUP BY p.pageUrl ORDER BY COUNT(DISTINCT p.session.sessionId) DESC")
    List<Object[]> getUniquePageViews(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(p) FROM PageVisit p WHERE p.isExitPage = true AND p.timestamp >= :startDate")
    Long countExitPages(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(p) FROM PageVisit p WHERE p.isBounce = true AND p.timestamp >= :startDate")
    Long countBounces(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT p.referrer, COUNT(p) FROM PageVisit p WHERE p.timestamp >= :startDate AND p.referrer IS NOT NULL AND p.referrer != '' GROUP BY p.referrer ORDER BY COUNT(p) DESC")
    List<Object[]> getTopReferrers(@Param("startDate") LocalDateTime startDate);
}
