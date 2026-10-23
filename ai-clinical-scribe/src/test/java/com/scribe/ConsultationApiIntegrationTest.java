package com.scribe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsPatientAndGeneratesSoapNote() throws Exception {
        String patientJson = """
                {
                  "name": "Jane Doe",
                  "age": 34,
                  "gender": "FEMALE",
                  "contactNumber": "+1 555 0100",
                  "medicalHistory": "No known drug allergies"
                }
                """;

        String patientResponse = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long patientId = objectMapper.readTree(patientResponse).path("data").path("id").asLong();

        String consultationJson = """
                {
                  "patientId": %d,
                  "rawTranscript": "Patient reports a sore throat and fever for three days. The temperature is 38.4 C and the throat is red. It looks like acute pharyngitis. I will prescribe amoxicillin 500 mg three times daily for 7 days. Follow up in one week."
                }
                """.formatted(patientId);

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consultationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.soapNote.subjective", containsString("sore throat")))
                .andExpect(jsonPath("$.data.soapNote.assessment", containsString("pharyngitis")))
                .andExpect(jsonPath("$.data.prescriptionItems[0].medicineName").value("amoxicillin"));

        mockMvc.perform(get("/api/v1/consultations").param("patientId", String.valueOf(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void rejectsInvalidPatientPayload() throws Exception {
        String patientJson = """
                {
                  "name": "",
                  "age": -1,
                  "gender": "FEMALE",
                  "contactNumber": "abc"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"));
    }
}
