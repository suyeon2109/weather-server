package com.gbsoft.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayMinMaxTempDto {
	private int cityId;
	private String date;
	private String maxTemp;
	private String minTemp;
	private String updateTime;
}
