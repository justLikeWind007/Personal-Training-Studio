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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRbacApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginAndGetProfile() throws Exception {
        String loginBody = """
                {
                  "mobile": "13800000001",
                  "password": "123456"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tenantId").value("tenant-demo"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        String token = jsonNode.path("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobile").value("13800000001"));
    }

    @Test
    void shouldListAndAssignRoles() throws Exception {
        mockMvc.perform(get("/api/rbac/roles")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("STORE_MANAGER"));

        mockMvc.perform(get("/api/rbac/roles/catalog")
                        .param("level", "HEADQUARTER")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("HEADQUARTER"))
                .andExpect(jsonPath("$[0].roleKey").value("HQ_ADMIN"));

        String req = """
                {
                  "roles": ["SALES", "RECEPTION"]
                }
                """;

        mockMvc.perform(post("/api/rbac/users/3001/roles")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3001))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void shouldConfigureRoleButtonsAndGetUserPermissionsWithVersion() throws Exception {
        mockMvc.perform(get("/api/rbac/permissions/catalog")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuKeys").isArray())
                .andExpect(jsonPath("$.buttonKeys").isArray());

        String configReq = """
                {
                  "menuKeys": ["dashboard", "stores", "rbac"],
                  "buttonKeys": ["store.create", "store.status", "rbac.assign"]
                }
                """;
        mockMvc.perform(put("/api/rbac/roles/STORE_MANAGER/permissions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleKey").value("STORE_MANAGER"))
                .andExpect(jsonPath("$.buttonKeys").isArray());

        String loginBody = """
                {
                  "mobile": "13800000002",
                  "password": "123456"
                }
                """;
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(loginResponse).path("token").asText();

        mockMvc.perform(get("/api/rbac/users/1002/permissions")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1002))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.buttonKeys").isArray())
                .andExpect(jsonPath("$.version").isNumber());
    }
}
