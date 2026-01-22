package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.ExecutionHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExecutionHistoryJpaRepository extends JpaRepository<ExecutionHistoryEntity, String> {

    List<ExecutionHistoryEntity> findByStrategyIdOrderByCreatedAtDesc(String strategyId);

    Page<ExecutionHistoryEntity> findByStrategyIdOrderByCreatedAtDesc(String strategyId, Pageable pageable);

    List<ExecutionHistoryEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    Page<ExecutionHistoryEntity> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    List<ExecutionHistoryEntity> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM ExecutionHistoryEntity e WHERE e.strategyId = :strategyId " +
           "AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    List<ExecutionHistoryEntity> findByStrategyIdAndDateRange(
            @Param("strategyId") String strategyId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e FROM ExecutionHistoryEntity e WHERE e.accountId = :accountId " +
           "AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    List<ExecutionHistoryEntity> findByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e FROM ExecutionHistoryEntity e WHERE " +
           "(:strategyId IS NULL OR e.strategyId = :strategyId) AND " +
           "(:accountId IS NULL OR e.accountId = :accountId) AND " +
           "(:executionType IS NULL OR e.executionType = :executionType) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    Page<ExecutionHistoryEntity> findByFilters(
            @Param("strategyId") String strategyId,
            @Param("accountId") String accountId,
            @Param("executionType") String executionType,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM ExecutionHistoryEntity e WHERE e.strategyId = :strategyId AND e.status = :status")
    long countByStrategyIdAndStatus(@Param("strategyId") String strategyId, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM ExecutionHistoryEntity e WHERE e.accountId = :accountId AND e.status = :status " +
           "AND e.createdAt BETWEEN :from AND :to")
    long countByAccountIdAndStatusAndCreatedAtBetween(
            @Param("accountId") String accountId,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e FROM ExecutionHistoryEntity e WHERE e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    List<ExecutionHistoryEntity> findByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
