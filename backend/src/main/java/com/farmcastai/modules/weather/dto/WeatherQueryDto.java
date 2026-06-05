package com.farmcastai.modules.weather.dto;

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
public class WeatherQueryDto {
    private double lat;
    private double lon;
    private int days;
    private boolean ai;
    private String units;
    private String lang;

    public Map<String, Object> toQueryParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("lat", lat);
        params.put("lon", lon);
        params.put("days", days);
        params.put("ai", ai);
        params.put("units", units);
        params.put("lang", lang);
        return params;
    }
}
