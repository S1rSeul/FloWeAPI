package com.floweapp.flowe_api;

import com.floweapp.flowe_api.calendar.dto.CreateEventRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Calendar API")
public class CalendarTest extends TestSupport {

    private static final String COUPLE_NOT_FOUND = "Пользователь не состоит в паре";
    private static final String COUPLE_NOT_ACTIVE = "Пара пользователя неактивна";

    @Test
    @DisplayName("CAL-01: Запрос календаря без access-токена даёт 401")
    void calendarWithoutTokenIsRejected() throws Exception {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusDays(1);

        MvcResult result = mockMvc.perform(request(HttpMethod.GET, CALENDAR_URL)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andReturn();

        assertRejected(result, 401);
    }

    @Test
    @DisplayName("CAL-02: Пользователь без пары не может получить календарь (404)")
    void userWithoutCoupleCannotGetCalendar() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(getCalendar(token, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1))
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("CAL-03: Пользователь с неактивной парой не может получить календарь (409)")
    void userWithPendingCoupleCannotGetCalendar() throws Exception {
        String token = registerAndGetAccessToken();
        createCoupleSuccessfully(token, uniqueCoupleNameRequestDto());

        assertRejected(getCalendar(token, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1))
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_ACTIVE))
                .andReturn(), 409);
    }

    @Test
    @DisplayName("CAL-04: Пустой календарь возвращает пустой список")
    void emptyCalendarReturnsEmptyList() throws Exception {
        String token = createActiveCouple().ownerToken();

        getCalendar(token, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("CAL-05: Общее событие видно обоим партнёрам")
    void sharedEventIsVisibleToBothPartners() throws Exception {
        ActiveCouple couple = createActiveCouple();
        OffsetDateTime startsAt = OffsetDateTime.now().plusHours(2);
        CreateEventRequestDto dto = uniqueCreateEventRequestDto(startsAt);

        JsonNode created = createEventSuccessfully(couple.ownerToken(), dto);

        getCalendar(couple.ownerToken(), startsAt.minusHours(1), startsAt.plusHours(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(created.path("id").asString()))
                .andExpect(jsonPath("$[0].type").value("event"))
                .andExpect(jsonPath("$[0].title").value(dto.title()))
                .andExpect(jsonPath("$[0].isMyPersonal").value(false))
                .andExpect(jsonPath("$[0].isPartnerPersonal").value(false));

        getCalendar(couple.partnerToken(), startsAt.minusHours(1), startsAt.plusHours(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(created.path("id").asString()))
                .andExpect(jsonPath("$[0].type").value("event"))
                .andExpect(jsonPath("$[0].isMyPersonal").value(false))
                .andExpect(jsonPath("$[0].isPartnerPersonal").value(false));
    }

    @Test
    @DisplayName("CAL-06: Личное событие видно только владельцу")
    void personalEventIsVisibleOnlyToOwner() throws Exception {
        ActiveCouple couple = createActiveCouple();
        OffsetDateTime startsAt = OffsetDateTime.now().plusHours(2);
        CreateEventRequestDto dto = new CreateEventRequestDto(
                "test-personal-event-" + UUID.randomUUID(),
                null,
                startsAt,
                startsAt.plusHours(1),
                false
        );

        JsonNode created = createEventSuccessfully(couple.ownerToken(), dto);

        getCalendar(couple.ownerToken(), startsAt.minusHours(1), startsAt.plusHours(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(created.path("id").asString()))
                .andExpect(jsonPath("$[0].type").value("event"))
                .andExpect(jsonPath("$[0].isMyPersonal").value(true))
                .andExpect(jsonPath("$[0].isPartnerPersonal").value(false));

        getCalendar(couple.partnerToken(), startsAt.minusHours(1), startsAt.plusHours(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(created.path("id").asString()))
                .andExpect(jsonPath("$[0].type").value("event"))
                .andExpect(jsonPath("$[0].isMyPersonal").value(false))
                .andExpect(jsonPath("$[0].isPartnerPersonal").value(true));
    }

    @Test
    @DisplayName("CAL-07: Дедлайн задачи попадает в календарь")
    void taskDeadlineIsVisibleInCalendar() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime dueDate = OffsetDateTime.now(ZoneOffset.UTC).plusHours(3);
        String taskId = createTaskWithDueDate(token, dueDate);

        getCalendar(token, dueDate.minusHours(1), dueDate.plusHours(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].type").value("task_deadline"))
                .andExpect(jsonPath("$[0].startsAt").isNotEmpty())
                .andExpect(jsonPath("$[0].endsAt").isEmpty())
                .andExpect(jsonPath("$[0].assignedToMe").value(false))
                .andExpect(jsonPath("$[0].assignedToPartner").value(false))
                .andExpect(jsonPath("$[0].status").value("todo"))
                .andExpect(jsonPath("$[0].isOverdue").value(false));
    }

    @Test
    @DisplayName("CAL-08: Задача без дедлайна не попадает в календарь")
    void taskWithoutDueDateIsNotInCalendar() throws Exception {
        String token = createActiveCouple().ownerToken();
        createTaskWithDueDate(token, null);

        getCalendar(token, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("CAL-09: Календарь фильтрует диапазон и сортирует по времени начала")
    void calendarFiltersRangeAndSortsItems() throws Exception {
        ActiveCouple couple = createActiveCouple();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusHours(4);
        OffsetDateTime to = from.plusDays(1);

        createEventSuccessfully(
                couple.ownerToken(),
                uniqueCreateEventRequestDto(from.minusHours(2))
        );
        createTaskWithDueDate(couple.ownerToken(), to.plusHours(1));

        JsonNode first = createEventSuccessfully(
                couple.ownerToken(),
                uniqueCreateEventRequestDto(from.plusHours(2))
        );
        String taskInRange = createTaskWithDueDate(couple.ownerToken(), from.plusHours(1));
        JsonNode last = createEventSuccessfully(
                couple.ownerToken(),
                uniqueCreateEventRequestDto(from.plusHours(4))
        );

        MvcResult result = getCalendar(couple.ownerToken(), from, to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        JsonNode items = responseJson(result);

        assertEquals(taskInRange, items.get(0).path("id").asString());
        assertEquals(first.path("id").asString(), items.get(1).path("id").asString());
        assertEquals(last.path("id").asString(), items.get(2).path("id").asString());

        assertEquals("task_deadline", items.get(0).path("type").asString());
        assertEquals("event", items.get(1).path("type").asString());
        assertEquals("event", items.get(2).path("type").asString());
    }

    @Test
    @DisplayName("CAL-10: Границы диапазона: from включается, to не включается")
    void calendarRangeBoundaries() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        OffsetDateTime to = from.plusDays(1);

        createEventSuccessfully(token, uniqueCreateEventRequestDto(from));
        createEventSuccessfully(token, uniqueCreateEventRequestDto(to));

        getCalendar(token, from, to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}