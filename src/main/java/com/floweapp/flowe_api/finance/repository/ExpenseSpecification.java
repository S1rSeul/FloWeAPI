package com.floweapp.flowe_api.finance.repository;

import com.floweapp.flowe_api.finance.entity.Expense;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExpenseSpecification {

    public static Specification<Expense> byCouple(UUID coupleId) {
        return (root, query, cb) -> cb.equal(root.get("coupleId"), coupleId);
    }

    public static Specification<Expense> byDateRange(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            if (from != null && to != null) return cb.between(root.get("spentAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("spentAt"), from);
            if (to != null) return cb.lessThanOrEqualTo(root.get("spentAt"), to);
            return cb.conjunction();
        };
    }

    public static Specification<Expense> byCategory(ExpenseCategory category) {
        return (root, query, cb) -> category == null ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> byCursor(OffsetDateTime cursorAt, UUID cursorId) {
        return (root, query, cb) -> {
            if (cursorAt == null || cursorId == null) return cb.conjunction();

            return cb.or(
                    cb.lessThan(root.get("spentAt"), cursorAt),
                    cb.and(
                            cb.equal(root.get("spentAt"), cursorAt),
                            cb.lessThan(root.get("id"), cursorId)
                    )
            );
        };
    }
}