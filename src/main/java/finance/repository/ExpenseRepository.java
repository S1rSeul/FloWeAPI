package finance.repository;

import finance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
