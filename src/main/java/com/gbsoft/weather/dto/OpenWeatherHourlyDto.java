package com.gbsoft.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OpenWeatherHourlyDto {
	private int cityId;
	private String current;
	private String hour1;
	private String hour2;
	private String hour3;
	private String hour4;
	private String hour5;
	private String hour6;
	private String updateTime;
}
