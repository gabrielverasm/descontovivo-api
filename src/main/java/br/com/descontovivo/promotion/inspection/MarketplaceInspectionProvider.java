package br.com.descontovivo.promotion.inspection;

public interface MarketplaceInspectionProvider {
    boolean supports(MarketplaceCode marketplace);
    MarketplaceInspectionData inspect(String url);
}
