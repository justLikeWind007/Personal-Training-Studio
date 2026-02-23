package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class MemberSegmentApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldFilterMembersAndExportCsv() throws Exception {
        long aliceId = createMember("Alice VIP", "13600001001", "VIP");
        long bobId = createMember("Bob Basic", "13600001002", "STANDARD");

        String updateBob = """
                {
                  "name": "Bob Basic",
                  "mobile": "13600001002",
                  "levelTag": "STANDARD",
                  "status": "SUSPENDED"
                }
                """;
        mockMvc.perform(put("/api/members/{id}", bobId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/members")
                        .param("status", "ACTIVE")
                        .param("levelTag", "VIP")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(aliceId))
                .andExpect(jsonPath("$[?(@.id==" + bobId + ")]").isEmpty());

        mockMvc.perform(get("/api/members")
                        .param("keyword", "Alice")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice VIP"));

        mockMvc.perform(get("/api/members/export")
                        .param("status", "ACTIVE")
                        .param("levelTag", "VIP")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Alice VIP")));
    }

    private long createMember(String name, String mobile, String levelTag) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "levelTag": "%s"
                }
                """.formatted(name, mobile, levelTag);
        String response = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }
}
