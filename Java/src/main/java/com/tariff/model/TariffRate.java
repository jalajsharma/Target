package com.tariff.model;

import java.time.LocalDate;
import java.util.Objects;

public class TariffRate {
    private String tariffId;
    private double tariffRate;
    private String level;
    private String entityId;
    private String countryCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String policyVersionId;

    // Constructors
    public TariffRate() {}

    public TariffRate(String tariffId, double tariffRate, String level, String entityId,
                      String countryCode, LocalDate startDate, LocalDate endDate,
                      String status, String policyVersionId) {
        this.tariffId = tariffId;
        this.tariffRate = tariffRate;
        this.level = level;
        this.entityId = entityId;
        this.countryCode = countryCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.policyVersionId = policyVersionId;
    }

    // Getters and Setters
    public String getTariffId() { return tariffId; }
    public void setTariffId(String tariffId) { this.tariffId = tariffId; }

    public double getTariffRate() { return tariffRate; }
    public void setTariffRate(double tariffRate) { this.tariffRate = tariffRate; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPolicyVersionId() { return policyVersionId; }
    public void setPolicyVersionId(String policyVersionId) { this.policyVersionId = policyVersionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TariffRate that = (TariffRate) o;
        return Objects.equals(tariffId, that.tariffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tariffId);
    }
}