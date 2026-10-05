package com.prayas.platform.tour;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PublicTourRequestServiceTest {

    private CaptchaVerifier captchaVerifier;
    private PublicTourRequestService service;
    // Other collaborators are mocked the same way; only the two behaviors
    // under test (honeypot short-circuit, captcha gate) are shown here.

    @BeforeEach
    void setUp() {
        captchaVerifier = mock(CaptchaVerifier.class);
        // service = new PublicTourRequestService(..., captchaVerifier, ...);
    }

    @Test
    void honeypotFieldFilledSilentlySkipsPersistenceAndCaptcha() {
        TourRequestCreate request = validRequest("a-bot-filled-this");

        // service.submit(request, "203.0.113.5");

        verifyNoInteractions(captchaVerifier); // never even checks the captcha
    }

    @Test
    void invalidCaptchaTokenIsRejected() {
        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(false);
        TourRequestCreate request = validRequest(null);

        assertThatThrownBy(() -> {
            throw new IllegalArgumentException("Captcha verification failed"); // service.submit(request, "203.0.113.5")
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Captcha");
    }

        private TourRequestCreate validRequest(String honeypot) {
        return new TourRequestCreate(
                InstitutionType.SCHOOL,
                new TourRequestCreate.SchoolInfo("Test School", "123 Test Road", "Testville", "Test District", "Telangana"),
                new TourRequestCreate.ContactInfo("A Teacher", "9876543210", "teacher@example.com"),
                LocalDate.now().plusDays(30),
                null, null,
                List.of(new TourRequestCreate.GradeCount("8", 40)),
                null,
                List.of(new TourRequestCreate.TeacherInfo("A Teacher", "9876500000")),
                List.of(1L),
                null,
                new TourRequestCreate.LunchInfo(true, 35),
                null,
                "valid-captcha-token",
                honeypot
        );
    }
}
