package com.gbsoft.weather.mybatis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWeatherVo {

    private Long cityId;
    private String current;
    private String Sido;
    private String weatherName;

    private String cityName;
    private String pm10;
    private String pm25;
    private String temp;
    private String reh;
    private String pop;
    private String cond;

    public void makeView() {

        cityName = Sido + " " + weatherName;

        String[] split = current.split(";");
        temp = split[0];
        reh  = split[1];
        pop  = split[2];
        cond = split[5];

        cityId = null;
        current = null;
        Sido = null;
        weatherName = null;
    }
}
