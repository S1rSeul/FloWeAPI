package com.floweapp.flowe_api.finance.repository;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.Expense;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndCoupleId(UUID id, UUID coupleId);

    @Query("""
                SELECT e FROM Expense e
                WHERE e.coupleId = :coupleId
                  AND e.spentAt >= :from
                  AND e.spentAt <= :to
            """)
    List<Expense> findByCoupleIdAndSpentAtBetween(
            @Param("coupleId") UUID coupleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
                SELECT e.currency, SUM(e.amount), SUM(e.user1PaidAmount), SUM(e.user2PaidAmount)
                FROM Expense e
                WHERE e.coupleId = :coupleId
                  AND e.spentAt >= :from
                  AND e.spentAt <= :to
                GROUP BY e.currency
            """)
    List<Object[]> getTotalSummaryNoCategory(
            @Param("coupleId") UUID coupleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
                SELECT e.currency, SUM(e.amount), SUM(e.user1PaidAmount), SUM(e.user2PaidAmount)
                FROM Expense e
                WHERE e.coupleId = :coupleId
                  AND e.spentAt >= :from
                  AND e.spentAt <= :to
                  AND e.category = :category
                GROUP BY e.currency
            """)
    List<Object[]> getTotalSummaryWithCategory(
            @Param("coupleId") UUID coupleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("category") ExpenseCategory category
    );

    @Query("""
                SELECT SUM(e.amount) FROM Expense e
                WHERE e.coupleId = :coupleId
                  AND e.category = :category
                  AND e.currency = :currency
                  AND e.spentAt >= :from
                  AND e.spentAt < :to
            """)
    BigDecimal sumByCategory(
            @Param("coupleId") UUID coupleId,
            @Param("category") ExpenseCategory category,
            @Param("currency") CurrencyCode currency,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
                SELECT SUM(e.amount) FROM Expense e
                WHERE e.coupleId = :coupleId
                  AND e.savingsAccountId = :savingsAccountId
                  AND e.currency = :currency
                  AND e.spentAt >= :from
                  AND e.spentAt < :to
            """)
    BigDecimal sumBySavingsAccount(
            @Param("coupleId") UUID coupleId,
            @Param("savingsAccountId") UUID savingsAccountId,
            @Param("currency") CurrencyCode currency,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
