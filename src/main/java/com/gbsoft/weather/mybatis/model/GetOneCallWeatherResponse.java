package com.gbsoft.weather.mybatis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class GetOneCallWeatherResponse {

    private AirInfo airInfo;
    private TodayInfo today;
    private CurrentInfo current;
    private Map<String, PerHour> hour;
    private Map<String, PerWeek> week;

    public static GetOneCallWeatherResponse of(GetOneCallWeatherVo getOneCallWeatherVo) {

        AirInfo airInfo = AirInfo.builder()
                .pm10(getOneCallWeatherVo.getPm10())
                .pm25(getOneCallWeatherVo.getPm25())
                .ozon(getOneCallWeatherVo.getOzon())
                .build();

        TodayInfo todayInfo = TodayInfo.builder()
                .maxTemp(getOneCallWeatherVo.getMaxTemp())
                .minTemp(getOneCallWeatherVo.getMinTemp())
                .build();

        CurrentInfo currentInfo = CurrentInfo.builder()
                .temp(getOneCallWeatherVo.getTemp())
                .pop(getOneCallWeatherVo.getPop())
                .humidity(getOneCallWeatherVo.getHumidity())
                .windSpeed(getOneCallWeatherVo.getWindSpeed())
                .windDirection(getOneCallWeatherVo.getWindDirection())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeather()))
                .build();

        Map<String, PerHour> hour = new LinkedHashMap<>();
        String[] split = getOneCallWeatherVo.getHour24().split(";");
        hour.put("24", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour03().split(";");
        hour.put("3", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour06().split(";");
        hour.put("6", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour09().split(";");
        hour.put("9", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour12().split(";");
        hour.put("12", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour15().split(";");
        hour.put("15", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour18().split(";");
        hour.put("18", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        split = getOneCallWeatherVo.getHour21().split(";");
        hour.put("21", PerHour.builder()
                .temp(split[0])
                .pop(split[1])
                .humidity(split[2])
                .windSpeed(split[3])
                .windDirection(split[4])
                .description(Weather.getCodeByDesc(split[5]))
                .build());

        Map<String, PerWeek> week = new LinkedHashMap<>();
        week.put("0", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT1())
                .maxTemp(getOneCallWeatherVo.getWeekTH1())
                .pop(getOneCallWeatherVo.getWeekPop1())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK1().replaceAll(" ", "")))
                .build());

        week.put("1", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT2())
                .maxTemp(getOneCallWeatherVo.getWeekTH2())
                .pop(getOneCallWeatherVo.getWeekPop2())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK2().replaceAll(" ", "")))
                .build());

        week.put("2", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT3())
                .maxTemp(getOneCallWeatherVo.getWeekTH3())
                .pop(getOneCallWeatherVo.getWeekPop3())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK3().replaceAll(" ", "")))
                .build());

        week.put("3", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT4())
                .maxTemp(getOneCallWeatherVo.getWeekTH4())
                .pop(getOneCallWeatherVo.getWeekPop4())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK4().replaceAll(" ", "")))
                .build());

        week.put("4", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT5())
                .maxTemp(getOneCallWeatherVo.getWeekTH5())
                .pop(getOneCallWeatherVo.getWeekPop5())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK5().replaceAll(" ", "")))
                .build());

        week.put("5", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT6())
                .maxTemp(getOneCallWeatherVo.getWeekTH6())
                .pop(getOneCallWeatherVo.getWeekPop6())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK6().replaceAll(" ", "")))
                .build());

        week.put("6", PerWeek.builder()
                .minTemp(getOneCallWeatherVo.getWeekT7())
                .maxTemp(getOneCallWeatherVo.getWeekTH7())
                .pop(getOneCallWeatherVo.getWeekPop7())
                .description(Weather.getCodeByDesc(getOneCallWeatherVo.getWeekK7().replaceAll(" ", "")))
                .build());

        return GetOneCallWeatherResponse.builder()
                .airInfo(airInfo)
                .today(todayInfo)
                .current(currentInfo)
                .hour(hour)
                .week(week)
                .build();
    }

    @Builder
    @Getter
    private static class AirInfo {
        private String pm10;
        private String pm25;
        private String ozon;
    }

    @Builder
    @Getter
    private static class TodayInfo {
        private String maxTemp;
        private String minTemp;
    }

    @Builder
    @Getter
    private static class CurrentInfo {
        private String temp;
        private String pop;
        private String humidity;
        private String windSpeed;
        private String windDirection;
        private Integer description;
    }

    @Builder
    @Getter
    private static class PerHour {
        private String temp;
        private String pop;
        private String humidity;
        private String windSpeed;
        private String windDirection;
        private Integer description;
    }

    @Builder
    @Getter
    private static class PerWeek {
        private String minTemp;
        private String maxTemp;
        private Object pop;
        private Integer description;
    }

}
