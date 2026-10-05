package com.prayas.platform.tour;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DecisionRequest(
        @NotNull DecisionType decision,
        String note,
        LocalDate proposedDate // required when decision == PROPOSE_DATE
) {
}
