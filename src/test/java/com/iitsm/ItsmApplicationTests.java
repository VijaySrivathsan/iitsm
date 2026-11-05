package com.iitsm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests: the Spring context starts and the public home page is served.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItsmApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Fails if any bean cannot be created.
    }

    @Test
    void homePage_isPublicAndReturns200() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Intelligent IT Service Management")));
    }

    @Test
    void protectedUrl_redirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection());
    }
}
