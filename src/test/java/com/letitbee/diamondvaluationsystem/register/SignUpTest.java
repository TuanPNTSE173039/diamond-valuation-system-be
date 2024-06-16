package com.letitbee.diamondvaluationsystem.register;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letitbee.diamondvaluationsystem.payload.AccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class SignUpTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    @Rollback
    public void testUsernameBlank() throws Exception {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setUsername("");
        String accountJson = objectMapper.writeValueAsString(accountDTO);

        mockMvc.perform(post("/api/v1/accounts/signup")
                        .contentType("application/json")
                        .content(accountJson))
                .andExpect(status().isBadRequest());
    }
}
