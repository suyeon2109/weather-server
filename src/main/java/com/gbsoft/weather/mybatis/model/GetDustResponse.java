package com.gbsoft.weather.mybatis.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GetDustResponse {
    private String pm10;
    private String pm25;
    private String pm10Grade;
    private String pm25Grade;
}
