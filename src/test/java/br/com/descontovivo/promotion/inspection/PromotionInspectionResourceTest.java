package br.com.descontovivo.promotion.inspection;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class PromotionInspectionResourceTest {
    @ParameterizedTest
    @CsvSource({
            "INVALID_URL,400",
            "UNSUPPORTED_MARKETPLACE,422",
            "INSPECTION_BLOCKED,422",
            "INSPECTION_FAILED,422",
            "IMPORTER_UNAVAILABLE,503"
    })
    void mapsStableErrorCodes(String code, int expectedStatus) {
        PromotionInspectionService service = mock(PromotionInspectionService.class);
        when(service.inspect(anyString())).thenThrow(new MarketplaceInspectionException(code, "message"));
        Response response = new PromotionInspectionResource(service)
                .inspect(new PromotionInspectionRequest("https://shopee.com.br/x"));
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(code, ((PromotionInspectionResource.ErrorResponse) response.getEntity()).code());
    }

    @Test
    void sanitizesUnexpectedFailuresAndReturnsRequestId() {
        PromotionInspectionService service = mock(PromotionInspectionService.class);
        when(service.inspect(anyString())).thenThrow(new IllegalStateException("sensitive detail"));

        Response response = new PromotionInspectionResource(service)
                .inspect(new PromotionInspectionRequest("https://shopee.com.br/x"));

        assertEquals(500, response.getStatus());
        var error = (PromotionInspectionResource.UnexpectedErrorResponse) response.getEntity();
        assertEquals("INTERNAL_ERROR", error.code());
        assertFalse(error.message().contains("sensitive detail"));
        assertFalse(error.requestId().isBlank());
    }
}
