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
class CrmFlowApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteLeadToMemberFlow() throws Exception {
        String token = loginToken();

        String createLeadReq = """
                {
                  "source": "DOUYIN",
                  "name": "Tom",
                  "mobile": "13900000001",
                  "ownerUserId": 1001
                }
                """;

        String leadResp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLeadReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long leadId = objectMapper.readTree(leadResp).path("id").asLong();

        String followReq = """
                {
                  "followType": "PHONE",
                  "content": "Call finished and invited to store",
                  "followerUserId": 1001
                }
                """;

        mockMvc.perform(post("/api/leads/{id}/follows", leadId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value(leadId));

        String memberResp = mockMvc.perform(post("/api/leads/{id}/convert-member", leadId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value(leadId))
                .andExpect(jsonPath("$.memberNo").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long memberId = objectMapper.readTree(memberResp).path("id").asLong();

        mockMvc.perform(get("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(memberId));

        mockMvc.perform(get("/api/members/{id}/timeline", memberId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MEMBER_CREATED"));

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].module").isNotEmpty());
    }

    private String loginToken() throws Exception {
        String loginBody = """
                {
                  "mobile": "13800000001",
                  "password": "123456"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        return jsonNode.path("token").asText();
    }
}
