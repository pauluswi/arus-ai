package com.pswied.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesPaymentRecord() throws Exception {
        String response = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("API-TEST-1001", "25.3000", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("INTERNAL"))
                .andExpect(jsonPath("$.amount").value(25.3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("API-TEST-1001"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void rejectsInvalidAmountsAndCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("API-TEST-INVALID-AMOUNT", "0", "USD")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("API-TEST-INVALID-CURRENCY", "1.00", "usd")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateSourceAndTransactionReference() throws Exception {
        String request = paymentJson("API-TEST-DUPLICATE", "10.00", "EUR");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void listsPaymentRecords() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private String paymentJson(String reference, String amount, String currency) {
        return """
                {
                  "source": "INTERNAL",
                  "transactionReference": "%s",
                  "endToEndId": "E2E-%s",
                  "amount": %s,
                  "currency": "%s",
                  "counterparty": "Test Counterparty",
                  "valueDate": "2026-11-18",
                  "status": "RECEIVED"
                }
                """.formatted(reference, reference, amount, currency);
    }
}
