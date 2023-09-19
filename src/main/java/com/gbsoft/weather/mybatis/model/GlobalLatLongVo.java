package com.gbsoft.weather.mybatis.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class GlobalLatLongVo {
	private int id;
	private int countryCode;
	private int cityCode;
	private double latitude;
	private double longitude;
	private String cityName;
	private int utc;
}
