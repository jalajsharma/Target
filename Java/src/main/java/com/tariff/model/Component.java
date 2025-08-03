package com.tariff.model;

import java.util.Objects;

public class Component {
    private String componentId;
    private String description;
    private String materialType;

    // Constructors
    public Component() {}

    public Component(String componentId, String description, String materialType) {
        this.componentId = componentId;
        this.description = description;
        this.materialType = materialType;
    }

    // Getters and Setters
    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Component component = (Component) o;
        return Objects.equals(componentId, component.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentId);
    }

    @Override
    public String toString() {
        return "Component{" +
                "componentId='" + componentId + '\'' +
                ", description='" + description + '\'' +
                ", materialType='" + materialType + '\'' +
                '}';
    }
}
