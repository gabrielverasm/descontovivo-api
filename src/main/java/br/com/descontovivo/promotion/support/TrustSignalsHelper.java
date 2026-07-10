package br.com.descontovivo.promotion.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Helper for trust signals processing across all marketplaces.
 * <p>
 * This class handles JSON serialization/deserialization and validation
 * of trust signals for multiple marketplaces.
 */
public class TrustSignalsHelper {

    private static final Logger LOG = Logger.getLogger(TrustSignalsHelper.class);

    private TrustSignalsHelper() {
        // Utility class
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Generic signals valid for all marketplaces
    private static final Set<String> GENERIC_SIGNALS = Set.of(
        "OFFICIAL_STORE",
        "HIGH_SALES",
        "GOOD_PRODUCT_RATING",
        "GOOD_SELLER_RATING",
        "PLATFORM_FULFILLED",
        "SOLD_BY_PLATFORM",
        "DELIVERED_BY_PLATFORM",
        "CURATED_BY_DESCONTOVIVO"
    );

    // Marketplace-specific signals
    private static final Map<String, Set<String>> MARKETPLACE_SIGNALS = new LinkedHashMap<>();

    static {
        // Amazon signals
        Set<String> amazonSignals = new HashSet<>();
        amazonSignals.add("AMAZON_PRIME");
        amazonSignals.add("AMAZON_A_TO_Z_GUARANTEE");
        MARKETPLACE_SIGNALS.put("AMAZON", amazonSignals);

        // Mercado Livre signals
        Set<String> mercadoLivreSignals = new HashSet<>();
        mercadoLivreSignals.add("MERCADO_LIVRE_COMPRA_GARANTIDA");
        mercadoLivreSignals.add("MERCADO_LIVRE_FULL");
        mercadoLivreSignals.add("MERCADO_LIDER");
        MARKETPLACE_SIGNALS.put("MERCADO_LIVRE", mercadoLivreSignals);

        // Magalu signals
        Set<String> magaluSignals = new HashSet<>();
        magaluSignals.add("MAGALU_GARANTE_COMPRA");
        magaluSignals.add("MAGALU_DEVOLUCAO_7_DIAS");
        MARKETPLACE_SIGNALS.put("MAGALU", magaluSignals);

        // Shopee signals
        Set<String> shopeeSignals = new HashSet<>();
        shopeeSignals.add("SHOPEE_GUARANTEE");
        MARKETPLACE_SIGNALS.put("SHOPEE", shopeeSignals);

        // AliExpress signals
        Set<String> aliExpressSignals = new HashSet<>();
        aliExpressSignals.add("ALIEXPRESS_BUYER_PROTECTION");
        aliExpressSignals.add("ALIEXPRESS_FREE_RETURN");
        aliExpressSignals.add("ALIEXPRESS_CHOICE");
        MARKETPLACE_SIGNALS.put("ALIEXPRESS", aliExpressSignals);
    }

    /**
     * Parse trust signals from JSON string to List.
     */
    public static List<String> parseTrustSignalsFromJson(String trustSignalsJson) {
        if (trustSignalsJson == null || trustSignalsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(trustSignalsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            LOG.warnf(e, "Failed to parse trust signals JSON: %s", trustSignalsJson);
            return List.of();
        }
    }

    /**
     * Convert trust signals list to JSON string.
     */
    public static String convertTrustSignalsToJson(List<String> trustSignals) {
        if (trustSignals == null || trustSignals.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(trustSignals);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize trust signals to JSON: %s", trustSignals);
            return null;
        }
    }

    /**
     * Validate trust signals for a specific marketplace.
     * Returns list of valid signals (filters out unknown signals).
     */
    public static List<String> validateTrustSignals(List<String> inputSignals, String marketplace) {
        if (inputSignals == null || inputSignals.isEmpty()) {
            return List.of();
        }

        List<String> validSignals = new ArrayList<>();
        for (String signal : inputSignals) {
            if (isValidTrustSignalForMarketplace(signal, marketplace)) {
                validSignals.add(signal);
            } else {
                LOG.warnf("Invalid trust signal '%s' for marketplace '%s' - ignoring", signal, marketplace);
            }
        }
        return validSignals;
    }

    /**
     * Check if a trust signal is valid for a specific marketplace.
     * Supports all 5 main marketplaces and generic signals.
     * Uses exact whitelist matching, not prefix-based matching.
     */
    public static boolean isValidTrustSignalForMarketplace(String signal, String marketplace) {
        if (signal == null || signal.isBlank()) {
            return false;
        }

        String normalizedSignal = signal.trim().toUpperCase();
        
        // Check generic signals first
        if (GENERIC_SIGNALS.contains(normalizedSignal)) {
            return true;
        }

        // Check marketplace-specific signals
        if (marketplace != null && !marketplace.isBlank()) {
            String normalizedMarketplace = marketplace.trim().toUpperCase();
            Set<String> marketplaceSignalSet = MARKETPLACE_SIGNALS.get(normalizedMarketplace);
            
            if (marketplaceSignalSet != null && marketplaceSignalSet.contains(normalizedSignal)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Derive trust signals from promotion fields.
     * This method can be used to auto-generate trust signals based on field values.
     */
    public static List<String> deriveTrustSignals(
            String marketplace,
            Integer salesCount,
            BigDecimal productRating,
            BigDecimal sellerRating,
            Boolean officialStore,
            List<String> existingTrustSignals) {
        
        List<String> signals = new ArrayList<>();
        
        // Add existing signals (filtered)
        if (existingTrustSignals != null) {
            for (String signal : existingTrustSignals) {
                if (isValidTrustSignalForMarketplace(signal, marketplace)) {
                    signals.add(signal);
                }
            }
        }
        
        // Derive from fields (add only if not already present)
        if (officialStore != null && officialStore) {
            String officialSignal = "OFFICIAL_STORE";
            if (!signals.contains(officialSignal)) {
                signals.add(officialSignal);
            }
        }
        
        if (salesCount != null && salesCount >= 1000) {
            String highSalesSignal = "HIGH_SALES";
            if (!signals.contains(highSalesSignal)) {
                signals.add(highSalesSignal);
            }
        }
        
        if (productRating != null && productRating.compareTo(new BigDecimal("4.7")) >= 0) {
            String goodProductRatingSignal = "GOOD_PRODUCT_RATING";
            if (!signals.contains(goodProductRatingSignal)) {
                signals.add(goodProductRatingSignal);
            }
        }
        
        if (sellerRating != null && sellerRating.compareTo(new BigDecimal("4.7")) >= 0) {
            String goodSellerRatingSignal = "GOOD_SELLER_RATING";
            if (!signals.contains(goodSellerRatingSignal)) {
                signals.add(goodSellerRatingSignal);
            }
        }
        
        return signals;
    }
}