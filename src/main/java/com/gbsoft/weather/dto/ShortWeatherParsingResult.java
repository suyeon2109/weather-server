package com.gbsoft.weather.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShortWeatherParsingResult {
	private Map<String, Object> todayWeather;
	private List<Map<String, Object>> hourWeather;
	private List<Map<String, Object>> weekWeather1;
	private Map<Integer,Map> every3hWeather;
	private String date;
	private String updateDate;
	private int cityId;
}
