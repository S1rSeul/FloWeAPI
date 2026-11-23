package com.floweapp.flowe_api;

import com.floweapp.flowe_api.finance.dto.CreateBudgetRequestDto;
import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Budgets API")
public class BudgetsTest extends TestSupport {

    private static final String COUPLE_NOT_FOUND = "Пользователь не состоит в паре";
    private static final String COUPLE_NOT_ACTIVE = "Пара пользователя неактивна";

    private CreateBudgetRequestDto categoryBudget(ExpenseCategory category, String limit, CurrencyCode currency) {
        return new CreateBudgetRequestDto(category, null, new BigDecimal(limit), currency);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("BUD-01: Запрос без access-токена даёт 401")
    void budgetsWithoutTokenAreRejected() throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.GET, BUDGETS_URL)).andReturn();
        assertRejected(result, 401);
    }

    @Test
    @DisplayName("BUD-02: Пользователь без пары получает 404")
    void userWithoutCoupleCannotCreateBudget() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(createBudget(token, categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB))
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_FOUND)).andReturn(), 404);
    }

    @Test
    @DisplayName("BUD-03: Пользователь с неактивной парой получает 409")
    void userWithPendingCoupleCannotCreateBudget() throws Exception {
        String token = registerAndGetAccessToken();
        createCoupleSuccessfully(token, uniqueCoupleNameRequestDto());

        assertRejected(createBudget(token, categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB))
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_ACTIVE)).andReturn(), 409);
    }

    @Test
    @DisplayName("BUD-04: Создание лимита на категорию, потрачено 0")
    void createCategoryBudget() throws Exception {
        String token = createActiveCouple().ownerToken();

        JsonNode body = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB));

        assertEquals("FOOD", body.path("category").asString());
        assertTrue(body.path("savingsAccountId").isNull());
        assertEquals("MONTHLY", body.path("period").asString());
        assertMoney("1000", body.path("limitAmount"));
        assertMoney("0", body.path("spentAmount"));
        assertMoney("1000", body.path("remainingAmount"));
        assertFalse(body.path("isExceeded").asBoolean());
    }

    @Test
    @DisplayName("BUD-05: Одновременно category и savingsAccountId даёт 400")
    void bothTargetsAreRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateBudgetRequestDto dto = new CreateBudgetRequestDto(
                ExpenseCategory.FOOD, UUID.randomUUID(), new BigDecimal("100"), CurrencyCode.RUB);

        assertRejected(createBudget(token, dto).andReturn(), 400);
    }

    @Test
    @DisplayName("BUD-06: Без category и savingsAccountId даёт 400")
    void noTargetIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateBudgetRequestDto dto = new CreateBudgetRequestDto(null, null, new BigDecimal("100"), CurrencyCode.RUB);

        assertRejected(createBudget(token, dto).andReturn(), 400);
    }

    @Test
    @DisplayName("BUD-07: Нулевой и отрицательный лимит даёт 400")
    void nonPositiveLimitIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();

        assertRejected(createBudget(token, categoryBudget(ExpenseCategory.FOOD, "0", CurrencyCode.RUB)).andReturn(), 400);
        assertRejected(createBudget(token, categoryBudget(ExpenseCategory.FOOD, "-5", CurrencyCode.RUB)).andReturn(), 400);
    }

    @Test
    @DisplayName("BUD-08: Дубль лимита на категорию и валюту даёт 409, другая валюта разрешена")
    void duplicateCategoryBudgetIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB));

        assertRejected(createBudget(token, categoryBudget(ExpenseCategory.FOOD, "2000", CurrencyCode.RUB)).andReturn(), 409);
        createBudget(token, categoryBudget(ExpenseCategory.FOOD, "50", CurrencyCode.USD))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("BUD-09: Потраченная сумма и остаток считаются по расходам категории")
    void spentAndRemainingAreCalculated() throws Exception {
        ActiveCouple couple = createActiveCouple();
        JsonNode budget = createBudgetSuccessfully(couple.ownerToken(),
                categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB));

        createExpenseSuccessfully(couple.ownerToken(), "300", CurrencyCode.RUB, ExpenseCategory.FOOD, now());
        createExpenseSuccessfully(couple.partnerToken(), "200", CurrencyCode.RUB, ExpenseCategory.FOOD, now());
        createExpenseSuccessfully(couple.ownerToken(), "999", CurrencyCode.RUB, ExpenseCategory.TRANSPORT, now());

        JsonNode body = responseJson(getBudget(couple.partnerToken(), budget.path("id").asString())
                .andExpect(status().isOk()).andReturn());

        assertMoney("500", body.path("spentAmount"));
        assertMoney("500", body.path("remainingAmount"));
        assertMoney("50", body.path("usagePercent"));
        assertFalse(body.path("isExceeded").asBoolean());
    }

    @Test
    @DisplayName("BUD-10: Превышение лимита: остаток 0 и isExceeded = true")
    void exceededBudget() throws Exception {
        String token = createActiveCouple().ownerToken();
        JsonNode budget = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB));

        createExpenseSuccessfully(token, "150", CurrencyCode.RUB, ExpenseCategory.FOOD, now());

        JsonNode body = responseJson(getBudget(token, budget.path("id").asString())
                .andExpect(status().isOk()).andReturn());

        assertMoney("150", body.path("spentAmount"));
        assertMoney("0", body.path("remainingAmount"));
        assertTrue(body.path("isExceeded").asBoolean());
    }

    @Test
    @DisplayName("BUD-11: Расход ровно на сумму лимита не считается превышением")
    void spentEqualToLimitIsNotExceeded() throws Exception {
        String token = createActiveCouple().ownerToken();
        JsonNode budget = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB));

        createExpenseSuccessfully(token, "100", CurrencyCode.RUB, ExpenseCategory.FOOD, now());

        JsonNode body = responseJson(getBudget(token, budget.path("id").asString())
                .andExpect(status().isOk()).andReturn());

        assertFalse(body.path("isExceeded").asBoolean());
        assertMoney("0", body.path("remainingAmount"));
    }

    @Test
    @DisplayName("BUD-12: Расходы в другой валюте не учитываются")
    void otherCurrencyIsIgnored() throws Exception {
        String token = createActiveCouple().ownerToken();
        JsonNode budget = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB));

        createExpenseSuccessfully(token, "500", CurrencyCode.USD, ExpenseCategory.FOOD, now());

        JsonNode body = responseJson(getBudget(token, budget.path("id").asString())
                .andExpect(status().isOk()).andReturn());
        assertMoney("0", body.path("spentAmount"));
    }

    @Test
    @DisplayName("BUD-13: Расходы прошлого месяца не учитываются, параметр month переключает период")
    void previousMonthIsExcludedUnlessRequested() throws Exception {
        String token = createActiveCouple().ownerToken();
        JsonNode budget = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "1000", CurrencyCode.RUB));
        String id = budget.path("id").asString();

        OffsetDateTime lastMonth = now().minusMonths(1).withDayOfMonth(15);
        createExpenseSuccessfully(token, "400", CurrencyCode.RUB, ExpenseCategory.FOOD, lastMonth);

        assertMoney("0", responseJson(getBudget(token, id).andReturn()).path("spentAmount"));

        String previous = YearMonth.from(lastMonth).toString();
        JsonNode previousBody = responseJson(mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get(BUDGETS_URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .param("month", previous)).andReturn());
        assertMoney("400", previousBody.path("spentAmount"));
    }

    @Test
    @DisplayName("BUD-14: Лимит на цель учитывает пополнения этой цели")
    void savingsBudgetCountsDeposits() throws Exception {
        String token = createActiveCouple().ownerToken();
        String accountId = createSavingsAccountSuccessfully(token, CurrencyCode.RUB);

        JsonNode budget = createBudgetSuccessfully(token, new CreateBudgetRequestDto(
                null, UUID.fromString(accountId), new BigDecimal("400"), CurrencyCode.RUB));

        deposit(token, accountId, "500");

        JsonNode body = responseJson(getBudget(token, budget.path("id").asString())
                .andExpect(status().isOk()).andReturn());
        assertMoney("500", body.path("spentAmount"));
        assertTrue(body.path("isExceeded").asBoolean());
        assertFalse(body.path("savingsAccountName").isNull());
    }

    @Test
    @DisplayName("BUD-15: Валюта лимита цели должна совпадать с валютой счёта (400)")
    void savingsBudgetCurrencyMismatch() throws Exception {
        String token = createActiveCouple().ownerToken();
        String accountId = createSavingsAccountSuccessfully(token, CurrencyCode.RUB);

        assertRejected(createBudget(token, new CreateBudgetRequestDto(
                null, UUID.fromString(accountId), new BigDecimal("100"), CurrencyCode.USD)).andReturn(), 400);
    }

    @Test
    @DisplayName("BUD-16: Дубль лимита на одну цель даёт 409")
    void duplicateSavingsBudgetIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        String accountId = createSavingsAccountSuccessfully(token, CurrencyCode.RUB);
        CreateBudgetRequestDto dto = new CreateBudgetRequestDto(
                null, UUID.fromString(accountId), new BigDecimal("100"), CurrencyCode.RUB);

        createBudgetSuccessfully(token, dto);
        assertRejected(createBudget(token, dto).andReturn(), 409);
    }

    @Test
    @DisplayName("BUD-17: Чужой накопительный счёт даёт 404")
    void foreignSavingsAccountIsRejected() throws Exception {
        String foreignOwner = createActiveCouple().ownerToken();
        String accountId = createSavingsAccountSuccessfully(foreignOwner, CurrencyCode.RUB);
        String token = createActiveCouple().ownerToken();

        assertRejected(createBudget(token, new CreateBudgetRequestDto(
                null, UUID.fromString(accountId), new BigDecimal("100"), CurrencyCode.RUB)).andReturn(), 404);
    }

    @Test
    @DisplayName("BUD-18: Изменение лимита пересчитывает остаток")
    void updateLimit() throws Exception {
        String token = createActiveCouple().ownerToken();
        JsonNode budget = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB));
        String id = budget.path("id").asString();
        createExpenseSuccessfully(token, "150", CurrencyCode.RUB, ExpenseCategory.FOOD, now());

        JsonNode body = responseJson(patchBudget(token, id, "{\"limitAmount\": 500}")
                .andExpect(status().isOk()).andReturn());

        assertMoney("500", body.path("limitAmount"));
        assertMoney("350", body.path("remainingAmount"));
        assertFalse(body.path("isExceeded").asBoolean());
    }

    @Test
    @DisplayName("BUD-19: Изменение лимита на неположительное значение даёт 400")
    void updateWithInvalidLimit() throws Exception {
        String token = createActiveCouple().ownerToken();
        String id = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB))
                .path("id").asString();

        assertRejected(patchBudget(token, id, "{\"limitAmount\": 0}").andReturn(), 400);
        assertRejected(patchBudget(token, id, "{}").andReturn(), 400);
    }

    @Test
    @DisplayName("BUD-20: Удаление лимита: 204, затем 404")
    void deleteBudget() throws Exception {
        String token = createActiveCouple().ownerToken();
        String id = createBudgetSuccessfully(token, categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB))
                .path("id").asString();

        deleteBudget(token, id).andExpect(status().isNoContent());
        assertRejected(getBudget(token, id).andReturn(), 404);
    }

    @Test
    @DisplayName("BUD-21: Лимиты чужой пары не видны и не изменяются")
    void budgetsAreIsolatedBetweenCouples() throws Exception {
        ActiveCouple first = createActiveCouple();
        ActiveCouple second = createActiveCouple();
        String id = createBudgetSuccessfully(first.ownerToken(),
                categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB)).path("id").asString();

        assertRejected(getBudget(second.ownerToken(), id).andReturn(), 404);
        assertRejected(patchBudget(second.ownerToken(), id, "{\"limitAmount\": 1}").andReturn(), 404);
        assertRejected(deleteBudget(second.ownerToken(), id).andReturn(), 404);
        getBudgets(second.ownerToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("BUD-22: Список лимитов виден обоим партнёрам, неверный month даёт 400")
    void listBudgets() throws Exception {
        ActiveCouple couple = createActiveCouple();
        createBudgetSuccessfully(couple.ownerToken(), categoryBudget(ExpenseCategory.FOOD, "100", CurrencyCode.RUB));
        createBudgetSuccessfully(couple.ownerToken(), categoryBudget(ExpenseCategory.HEALTH, "100", CurrencyCode.RUB));

        getBudgets(couple.partnerToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        assertRejected(getBudgets(couple.partnerToken(), "not-a-month").andReturn(), 400);
    }
}