package com.floweapp.flowe_api;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Expense history API")
public class ExpenseHistoryTest extends TestSupport {

    private OffsetDateTime day(int daysAgo) {
        return OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo);
    }

    @Test
    @DisplayName("EXH-01: История без токена даёт 401")
    void historyWithoutTokenIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.GET, EXPENSES_URL)).andReturn();
        assertRejected(result, 401);
    }

    @Test
    @DisplayName("EXH-02: Пользователь без пары получает 404, с неактивной парой 409")
    void historyRequiresActiveCouple() throws Exception {
        String lonely = registerAndGetAccessToken();
        assertRejected(listExpenses(lonely, null, null, null, null, null).andReturn(), 404);

        String pending = registerAndGetAccessToken();
        createCoupleSuccessfully(pending, uniqueCoupleNameRequestDto());
        assertRejected(listExpenses(pending, null, null, null, null, null).andReturn(), 409);
    }

    @Test
    @DisplayName("EXH-03: Пустая история возвращает пустой список")
    void emptyHistory() throws Exception {
        String token = createActiveCouple().ownerToken();

        listExpenses(token, null, null, null, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.totalSummary.length()").value(0));
    }

    @Test
    @DisplayName("EXH-04: Фильтр по категории и итог по фильтру")
    void filterByCategory() throws Exception {
        String token = createActiveCouple().ownerToken();
        createExpenseSuccessfully(token, "100", CurrencyCode.RUB, ExpenseCategory.FOOD, day(1));
        createExpenseSuccessfully(token, "50", CurrencyCode.RUB, ExpenseCategory.FOOD, day(2));
        createExpenseSuccessfully(token, "999", CurrencyCode.RUB, ExpenseCategory.TRANSPORT, day(1));

        JsonNode body = responseJson(listExpenses(token, null, null, ExpenseCategory.FOOD, null, null)
                .andExpect(status().isOk()).andReturn());

        assertEquals(2, body.path("items").size());
        body.path("items").forEach(item -> assertEquals("FOOD", item.path("category").asString()));
        assertMoney("150", body.path("totalSummary").get(0).path("totalAmount"));
    }

    @Test
    @DisplayName("EXH-05: Фильтр по периоду включает границы")
    void filterByPeriod() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime inside = day(5);
        createExpenseSuccessfully(token, "10", CurrencyCode.RUB, ExpenseCategory.FOOD, day(20));
        createExpenseSuccessfully(token, "20", CurrencyCode.RUB, ExpenseCategory.FOOD, inside);
        createExpenseSuccessfully(token, "30", CurrencyCode.RUB, ExpenseCategory.FOOD, day(1));

        JsonNode body = responseJson(listExpenses(token, day(6), day(3), null, null, null)
                .andExpect(status().isOk()).andReturn());

        assertEquals(1, body.path("items").size());
        assertMoney("20", body.path("totalSummary").get(0).path("totalAmount"));
    }

    @Test
    @DisplayName("EXH-06: Период и категория применяются вместе")
    void filterByPeriodAndCategory() throws Exception {
        String token = createActiveCouple().ownerToken();
        createExpenseSuccessfully(token, "10", CurrencyCode.RUB, ExpenseCategory.FOOD, day(2));
        createExpenseSuccessfully(token, "20", CurrencyCode.RUB, ExpenseCategory.HEALTH, day(2));
        createExpenseSuccessfully(token, "30", CurrencyCode.RUB, ExpenseCategory.FOOD, day(30));

        JsonNode body = responseJson(listExpenses(token, day(5), day(0).plusHours(1), ExpenseCategory.FOOD, null, null)
                .andExpect(status().isOk()).andReturn());

        assertEquals(1, body.path("items").size());
        assertMoney("10", body.path("totalSummary").get(0).path("totalAmount"));
    }

    @Test
    @DisplayName("EXH-07: Сортировка от новых к старым")
    void sortedByDateDescending() throws Exception {
        String token = createActiveCouple().ownerToken();
        createExpenseSuccessfully(token, "1", CurrencyCode.RUB, ExpenseCategory.FOOD, day(3));
        createExpenseSuccessfully(token, "2", CurrencyCode.RUB, ExpenseCategory.FOOD, day(1));
        createExpenseSuccessfully(token, "3", CurrencyCode.RUB, ExpenseCategory.FOOD, day(2));

        JsonNode items = responseJson(listExpenses(token, null, null, null, null, null)
                .andExpect(status().isOk()).andReturn()).path("items");

        OffsetDateTime previous = null;
        for (JsonNode item : items) {
            OffsetDateTime current = OffsetDateTime.parse(item.path("spentAt").asString());
            if (previous != null) assertFalse(current.isAfter(previous), "Записи должны идти по убыванию даты");
            previous = current;
        }
    }

    @Test
    @DisplayName("EXH-08: Курсорная пагинация не теряет и не дублирует записи")
    void cursorPagination() throws Exception {
        String token = createActiveCouple().ownerToken();
        for (int i = 1; i <= 5; i++) {
            createExpenseSuccessfully(token, String.valueOf(i), CurrencyCode.RUB, ExpenseCategory.FOOD, day(i));
        }

        Set<String> seen = new HashSet<>();
        String cursor = null;
        int pages = 0;
        boolean hasMore;
        do {
            JsonNode page = responseJson(listExpenses(token, null, null, null, 2, cursor)
                    .andExpect(status().isOk()).andReturn());
            page.path("items").forEach(item -> assertTrue(seen.add(item.path("id").asString()), "Дубль записи"));
            hasMore = page.path("hasMore").asBoolean();
            cursor = hasMore ? page.path("nextCursor").asString() : null;
            pages++;
            assertTrue(pages <= 3, "Слишком много страниц");
        } while (hasMore);

        assertEquals(5, seen.size());
        assertEquals(3, pages);
    }

    @Test
    @DisplayName("EXH-09: Итог по фильтру не зависит от размера страницы")
    void totalSummaryCoversAllPages() throws Exception {
        String token = createActiveCouple().ownerToken();
        for (int i = 1; i <= 3; i++) {
            createExpenseSuccessfully(token, "100", CurrencyCode.RUB, ExpenseCategory.FOOD, day(i));
        }

        JsonNode body = responseJson(listExpenses(token, null, null, null, 1, null)
                .andExpect(status().isOk()).andReturn());

        assertEquals(1, body.path("items").size());
        assertMoney("300", body.path("totalSummary").get(0).path("totalAmount"));
    }

    @Test
    @DisplayName("EXH-10: Некорректные limit, cursor, период и категория дают 400")
    void invalidQueryParameters() throws Exception {
        String token = createActiveCouple().ownerToken();

        assertRejected(listExpenses(token, null, null, null, 0, null).andReturn(), 400);
        assertRejected(listExpenses(token, null, null, null, 101, null).andReturn(), 400);
        assertRejected(listExpenses(token, null, null, null, null, "garbage").andReturn(), 400);
        assertRejected(listExpenses(token, day(0), day(5), null, null, null).andReturn(), 400);
        assertRejected(mockMvc.perform(get(EXPENSES_URL)
                .header("Authorization", "Bearer " + token)
                .param("category", "NOT_A_CATEGORY")).andReturn(), 400);
    }

    @Test
    @DisplayName("EXH-11: История видна обоим партнёрам, плательщики отражаются как я/партнёр")
    void historyIsSharedBetweenPartners() throws Exception {
        ActiveCouple couple = createActiveCouple();
        createExpense(couple.ownerToken(), "100", "60", "40",
                CurrencyCode.RUB, ExpenseCategory.FOOD, day(1)).andExpect(status().isCreated());

        JsonNode ownerItem = responseJson(listExpenses(couple.ownerToken(), null, null, null, null, null)
                .andReturn()).path("items").get(0);
        JsonNode partnerItem = responseJson(listExpenses(couple.partnerToken(), null, null, null, null, null)
                .andReturn()).path("items").get(0);

        assertMoney("60", ownerItem.path("myPaidAmount"));
        assertMoney("40", ownerItem.path("partnerPaidAmount"));
        assertMoney("40", partnerItem.path("myPaidAmount"));
        assertMoney("60", partnerItem.path("partnerPaidAmount"));
    }

    @Test
    @DisplayName("EXH-12: Расходы чужой пары не видны, GET по id даёт 404")
    void historyIsIsolatedBetweenCouples() throws Exception {
        ActiveCouple first = createActiveCouple();
        ActiveCouple second = createActiveCouple();
        String expenseId = createExpenseSuccessfully(first.ownerToken(), "100",
                CurrencyCode.RUB, ExpenseCategory.FOOD, day(1)).path("id").asString();

        listExpenses(second.ownerToken(), null, null, null, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        assertRejected(mockMvc.perform(get(EXPENSES_URL + "/" + expenseId)
                .header("Authorization", "Bearer " + second.ownerToken())).andReturn(), 404);
    }
}