package com.gbsoft.weather.mybatis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Getter
public enum Weather {

    CLEAR(0, "맑음"),
    GRAY(1, "흐림"),
    CLOUDY(2, "구름많음"),
    CLOUDY_RAIN(3, "흐리고 비"),
    RAIN_SNOW(4, "비/눈"),
    RAIN(5, "비"),
    SNOW(6, "눈"),
    //CLOUDY_RAIN(7, "구름많고 비"), //사용 x
    SHOWER(8, "소나기"),
    //CLOUDY_SHOWER(9, "구름많고 소나기"), //사용 x
    LIGHTENING(10, "천둥번개"),
    FOGGY(11, "안개")
    ;

    private int code;
    private String desc;


    public static int getCodeByDesc(String desc) {
        Optional<Weather> first = Arrays.asList(values()).stream()
                .filter(v -> v.desc.equals(desc))
                .findFirst();

        if (first.isPresent()) {
            return first.get().code;
        } else {
            return -1;
        }



    }


}
