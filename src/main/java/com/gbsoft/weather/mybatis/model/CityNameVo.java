package com.gbsoft.weather.mybatis.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class CityNameVo {
	private String weatherName;
	private String dustName;
	private int x;
	private int y;
	private String rss_short;
}
