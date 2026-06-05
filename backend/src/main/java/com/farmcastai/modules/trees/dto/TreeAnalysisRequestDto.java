package com.farmcastai.modules.trees.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeAnalysisRequestDto {
    private String farmerId;
    private String county;
    private Double landAcres;
    private String location;
    private String notes;

    public Map<String, Object> toFormFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("farmerId", farmerId);
        fields.put("county", county);
        fields.put("landAcres", landAcres);
        fields.put("location", location);
        fields.put("notes", notes);
        return fields;
    }
}
