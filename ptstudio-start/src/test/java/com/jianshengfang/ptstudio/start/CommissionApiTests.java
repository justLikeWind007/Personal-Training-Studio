package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommissionApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateRuleGenerateAndLockStatements() throws Exception {
        createCoach("Coach E", "13700000021");
        createCoach("Coach F", "13700000022");

        String ruleReq = """
                {
                  "name": "Standard Rule",
                  "calcMode": "PERCENT",
                  "ratio": 0.30,
                  "effectiveFrom": "%s"
                }
                """.formatted(OffsetDateTime.now().toString());
        String ruleResp = mockMvc.perform(post("/api/commission/rules")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long ruleId = objectMapper.readTree(ruleResp).path("id").asLong();

        String genReq = """
                {
                  "statementMonth": "2026-02",
                  "ruleId": %d
                }
                """.formatted(ruleId);
        String genResp = mockMvc.perform(post("/api/commission/statements/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(genReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("GENERATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long statementId = objectMapper.readTree(genResp).get(0).path("id").asLong();

        mockMvc.perform(post("/api/commission/statements/{id}/lock", statementId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        mockMvc.perform(get("/api/commission/statements")
                        .param("statementMonth", "2026-02")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private void createCoach(String name, String mobile) throws Exception {
        String req = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "level": "L2",
                  "specialties": "strength"
                }
                """.formatted(name, mobile);
        mockMvc.perform(post("/api/coaches")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());
    }
}
