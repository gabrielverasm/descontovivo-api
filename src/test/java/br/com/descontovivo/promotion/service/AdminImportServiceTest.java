package br.com.descontovivo.promotion.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminImportServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-15T12:00:00-03:00");

    @Test
    void publicationLessThan24HoursAgoBlocksDuplicate() {
        assertTrue(AdminImportService.isWithinDuplicateWindow(NOW.minusHours(23).minusMinutes(59), NOW));
    }

    @Test
    void publicationExactly24HoursAgoAllowsDuplicate() {
        assertFalse(AdminImportService.isWithinDuplicateWindow(NOW.minusHours(24), NOW));
    }

    @Test
    void publicationMoreThan24HoursAgoAllowsDuplicate() {
        assertFalse(AdminImportService.isWithinDuplicateWindow(NOW.minusHours(24).minusNanos(1), NOW));
    }

    @Test
    void publicationFiveDaysAgoAllowsDuplicate() {
        assertFalse(AdminImportService.isWithinDuplicateWindow(NOW.minusDays(5), NOW));
    }

    @Test
    void missingPublicationDateAllowsDuplicate() {
        assertFalse(AdminImportService.isWithinDuplicateWindow(null, NOW));
    }

    @Test
    void futurePublicationDateAllowsDuplicate() {
        assertFalse(AdminImportService.isWithinDuplicateWindow(NOW.plusSeconds(1), NOW));
    }
}
