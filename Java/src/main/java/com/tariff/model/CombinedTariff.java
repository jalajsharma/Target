package com.tariff.model;

import com.tariff.enums.TariffCombinationPolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CombinedTariff {
    private String itemId;
    private String countryCode;
    private BigDecimal itemTariffRate;
    private BigDecimal componentTariffRate;
    private BigDecimal finalTariffRate;
    private TariffCombinationPolicy combinationPolicy;
    private List<String> componentsUsed;
    private LocalDateTime calculationTimestamp;

    // Constructors
    public CombinedTariff() {}

    public CombinedTariff(String itemId, String countryCode, BigDecimal itemTariffRate,
                          BigDecimal componentTariffRate, BigDecimal finalTariffRate,
                          TariffCombinationPolicy combinationPolicy, List<String> componentsUsed,
                          LocalDateTime calculationTimestamp) {
        this.itemId = itemId;
        this.countryCode = countryCode;
        this.itemTariffRate = itemTariffRate;
        this.componentTariffRate = componentTariffRate;
        this.finalTariffRate = finalTariffRate;
        this.combinationPolicy = combinationPolicy;
        this.componentsUsed = componentsUsed;
        this.calculationTimestamp = calculationTimestamp;
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public BigDecimal getItemTariffRate() { return itemTariffRate; }
    public void setItemTariffRate(BigDecimal itemTariffRate) { this.itemTariffRate = itemTariffRate; }

    public BigDecimal getComponentTariffRate() { return componentTariffRate; }
    public void setComponentTariffRate(BigDecimal componentTariffRate) { this.componentTariffRate = componentTariffRate; }

    public BigDecimal getFinalTariffRate() { return finalTariffRate; }
    public void setFinalTariffRate(BigDecimal finalTariffRate) { this.finalTariffRate = finalTariffRate; }

    public TariffCombinationPolicy getCombinationPolicy() { return combinationPolicy; }
    public void setCombinationPolicy(TariffCombinationPolicy combinationPolicy) { this.combinationPolicy = combinationPolicy; }

    public List<String> getComponentsUsed() { return componentsUsed; }
    public void setComponentsUsed(List<String> componentsUsed) { this.componentsUsed = componentsUsed; }

    public LocalDateTime getCalculationTimestamp() { return calculationTimestamp; }
    public void setCalculationTimestamp(LocalDateTime calculationTimestamp) { this.calculationTimestamp = calculationTimestamp; }
}