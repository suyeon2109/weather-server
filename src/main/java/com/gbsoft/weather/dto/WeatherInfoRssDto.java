package com.gbsoft.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherInfoRssDto {
	private String date;
	private double temperature;
	private int precipitationProbability;
	private int humidity;
	private String windSpeed;
	private String windDirection;
	private String weather;

	private String hour24;
	private String hour03;
	private String hour06;
	private String hour09;
	private String hour12;
	private String hour15;
	private String hour18;
	private String hour21;

	private String updateTime;
	private int cityId;
}
