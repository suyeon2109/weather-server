package com.gbsoft.weather.dto;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GlobalWeatherParsingResult {
	private int today;
	private List<JSONObject> week;
	private Long currentTemp;
	private int currentHumidity;
	private String currentMain;
	private String currentDesc;
	private String currentIcon;
	private List<JSONObject> hour;
	private Map<String, String> weather;
}
