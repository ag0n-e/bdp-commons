package it.bz.odh.trafficprovbz.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MetadataDto {

	@JsonProperty("Id")
	private int id;

	@JsonProperty("Nome")
	private String name;

	@JsonProperty("SchemiDiClassificazione")
	private int classificationSchema;

	private final Map<String, Object> otherFields = new HashMap<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getClassificationSchema() {
		return this.classificationSchema;
	}

	public void setClassificationSchema(int classificationSchema) {
		this.classificationSchema = classificationSchema;
	}

    // Capture all other fields that Jackson do not match
    @JsonAnyGetter
    public Map<String, Object> getOtherFields() {
        return otherFields;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        otherFields.put(name, value);
    }
}
