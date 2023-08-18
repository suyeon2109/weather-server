package com.gbsoft.weather.mybatis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetDustVo {
    private String FineDustConcentration;
    private String UltrafineDustConcentration;
}
