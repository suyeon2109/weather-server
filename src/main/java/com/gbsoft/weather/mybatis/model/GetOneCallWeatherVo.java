package com.gbsoft.weather.mybatis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetOneCallWeatherVo {

    private int cityId;
    private String pm10;
    private String pm25;
    private String ozon;
    private String maxTemp;
    private String minTemp;
    private String temp;
    private String pop;
    private String humidity;
    private String windSpeed;
    private String windDirection;
    private String Weather;
    private String hour24;
    private String hour03;
    private String hour06;
    private String hour09;
    private String hour12;
    private String hour15;
    private String hour18;
    private String hour21;

    private String weekT1;
    private String weekTH1;
    private String weekPop1;
    private String weekK1;

    private String weekT2;
    private String weekTH2;
    private Integer weekPop2;
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

    public void makeView() {



    }
}
