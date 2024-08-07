package com.letitbee.diamondvaluationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.letitbee.diamondvaluationsystem.payload.AccountResponse;
import com.letitbee.diamondvaluationsystem.payload.CustomerDTO;
import com.letitbee.diamondvaluationsystem.payload.CustomerUpdate;
import com.letitbee.diamondvaluationsystem.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(authorities = {"CUSTOMER"})
public class UpdateProfileTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;


    @Test
    @Transactional
    @Rollback
    public void testFirstnameBlank() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("");
        customerDTO.setLastName("testLastname");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());
    }



    @Test
    @Transactional
    @Rollback
    public void testFirstNameIsNot2To24CharacterS() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("t");
        customerDTO.setLastName("testLastname");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Transactional
    @Rollback
    public void testFirstNameIsContainNumberOrSpecialCharacter() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("d@nny1");
        customerDTO.setLastName("testLastname");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());
    }



    @Test
    @Transactional
    @Rollback
    public void testLastNameBlank() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("testFirstname");
        customerDTO.setLastName("");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @Rollback
    public void testLastNameIsNot2To24CharacterS() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("testFirstname");
        customerDTO.setLastName("t");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Transactional
    @Rollback
    public void testLastNameIsContainNumberOrSpecialCharacter() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("testFirstname");
        customerDTO.setLastName("d@nny1");
        customerDTO.setPhone("testPhone");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @Rollback
    public void testPhoneBlank() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("testFirstname");
        customerDTO.setLastName("testLastname");
        customerDTO.setPhone("");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @Rollback
    public void testPhoneIsNot10Digits() throws Exception {

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setFirstName("testFirstname");
        customerDTO.setLastName("testLastname");
        customerDTO.setPhone("123456789");
        customerDTO.setAddress("testAddress");
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(Long.valueOf(1));
        customerDTO.setAccount(accountResponse);

        String customerJson = objectMapper.writeValueAsString(customerDTO);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType("application/json")
                        .content(customerJson))
                .andExpect(status().isBadRequest());

    }


    @Test
    @Transactional
    @Rollback
    public void testPhoneExist() throws Exception {

        CustomerUpdate customerUpdate = new CustomerUpdate();
        customerUpdate.setPhone("0368214667");
        String customerJson = objectMapper.writeValueAsString(customerUpdate);
        if(customerRepository.existsByPhone(customerUpdate.getPhone())){
            System.out.println("Phone already exist");
            mockMvc.perform(put("/api/v1/customers/1")
                            .contentType("application/json")
                            .content(customerJson))
                    .andExpect(status().isBadRequest());
        }
        else {
            System.out.println("Update successfully");
            mockMvc.perform(put("/api/v1/customers/1")
                            .contentType("application/json")
                            .content(customerJson))
                    .andExpect(status().isOk());
        }
    }

}