package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleCoachSlotApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateCoachAndSlot() throws Exception {
        String coachReq = """
                {
                  "name": "Coach Lin",
                  "mobile": "13700000001",
                  "level": "L2",
                  "specialties": "strength"
                }
                """;

        String coachResp = mockMvc.perform(post("/api/coaches")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(coachReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Coach Lin"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long coachId = objectMapper.readTree(coachResp).path("id").asLong();

        String slotReq = """
                {
                  "slotDate": "2026-02-23",
                  "startTime": "10:00:00",
                  "endTime": "11:00:00",
                  "capacity": 2
                }
                """;

        mockMvc.perform(post("/api/coaches/{id}/slots", coachId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coachId").value(coachId))
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(get("/api/coaches/{id}/slots", coachId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capacity").value(2));
    }
}
