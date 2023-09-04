package com.gbsoft.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherInfoDto {
	private String date;
	private double temperature;
	private String weather;
	private int precipitationProbability;
	private String windSpeed;
	private String windDirection;
	private int humidity;

	private int hourT1;
	private String hourTH1;
	private String hourK1;
	private int hourT2;
	private String hourTH2;
	private String hourK2;
	private int hourT3;
	private String hourTH3;
	private String hourK3;

	private String weekT1;
	private String weekTH1;
	private int weekPop1;
	private String weekK1;
	private String weekT2;
	private String weekTH2;
	private int weekPop2;
	private String weekK2;

	private String weekT3;
	private String weekTH3;
	private String weekPop3;
	private String weekK3;

	private String weekT4;
	private String weekTH4;
	private String weekPop4;
	private String weekK4;

	private String weekT5;
	private String weekTH5;
	private String weekPop5;
	private String weekK5;

	private String weekT6;
	private String weekTH6;
	private String weekPop6;
	private String weekK6;

	private String weekT7;
	private String weekTH7;
	private String weekPop7;
	private String weekK7;

	private String updateTime;
	private int cityId;
}
