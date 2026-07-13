package br.com.descontovivo.promotion.inspection;

public class MarketplaceInspectionException extends RuntimeException {
    private final String code;

    public MarketplaceInspectionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
