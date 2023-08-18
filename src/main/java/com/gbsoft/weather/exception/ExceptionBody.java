package com.gbsoft.weather.exception;

import lombok.Getter;

@Getter
public class ExceptionBody {


    private String pm10;
    private String pm25;
    private String pm10Grade;
    private String pm25Grade;

    public ExceptionBody() {
        pm10 = "-";
        pm25 = "-";
        pm10Grade = "-";
        pm25Grade = "-";
    }
}
