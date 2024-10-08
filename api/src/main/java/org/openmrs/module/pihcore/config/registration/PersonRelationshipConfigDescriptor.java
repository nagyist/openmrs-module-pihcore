package org.openmrs.module.pihcore.config.registration;

import org.codehaus.jackson.annotate.JsonProperty;

public class PersonRelationshipConfigDescriptor {

    @JsonProperty
    private String id;

    @JsonProperty
    private String relationshipType;

    @JsonProperty
    private String relationshipDirection;

    @JsonProperty
    private Boolean multipleValues = false;

    @JsonProperty
    private String label;

    @JsonProperty
    private String gender;

    @JsonProperty
    private Boolean required = false;

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipDirection() {
        return relationshipDirection;
    }

    public void setRelationshipDirection(String relationshipDirection) {
        this.relationshipDirection = relationshipDirection;
    }

    public Boolean getMultipleValues() {
        return multipleValues;
    }

    public void setMultipleValues(Boolean multipleValues) {
        this.multipleValues = multipleValues;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
