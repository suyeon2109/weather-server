package com.gbsoft.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GlobalWeatherDto {
	private int id;
	private String updateTimeKr;
	private int countryCode;
	private int cityCode;
	private Long pm25;
	private Long pm10;
	private int today;
	private Long minTemp;
	private Long maxTemp;
	private Long currentTemp;
	private String currentMain;
	private String currentDesc;
	private String currentIcon;
	private int hour1Time;
	private Long hour1Temp;
	private String hour1Icon;
	private int hour2Time;
	private Long hour2Temp;
	private String hour2Icon;
	private int hour3Time;
	private Long hour3Temp;
	private String hour3Icon;
	private Long week1TMax;
	private Long week1TMin;
	private String week1Desc;
	private String week1Icon;
	private Long week2TMax;
	private Long week2TMin;
	private String week2Desc;
	private String week2Icon;
	private Long week3TMax;
	private Long week3TMin;
	private String week3Desc;
	private String week3Icon;
	private Long week4TMax;
	private Long week4TMin;
	private String week4Desc;
	private String week4Icon;
	private Long week5TMax;
	private Long week5TMin;
	private String week5Desc;
	private String week5Icon;
	private Long week6TMax;
	private Long week6TMin;
	private String week6Desc;
	private String week6Icon;
	private Long week7TMax;
	private Long week7TMin;
	private String week7Desc;
	private String week7Icon;
	private int currentHumidity;
	private String valueOf12am;
	private String valueOf3am;
	private String valueOf6am;
	private String valueOf9am;
	private String valueOf12pm;
	private String valueOf3pm;
	private String valueOf6pm;
	private String valueOf9pm;
	private String week1;
	private String week2;
	private String week3;
	private String week4;
	private String week5;
	private String week6;
	private String week7;







}
