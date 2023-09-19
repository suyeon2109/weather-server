package com.gbsoft.weather.service;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gbsoft.weather.exception.NoSuchAirDataException;
import com.gbsoft.weather.exception.NoSuchCityException;
import com.gbsoft.weather.mybatis.mapper.WeatherMapper;
import com.gbsoft.weather.mybatis.model.CityNameVo;
import com.gbsoft.weather.mybatis.model.GetDustResponse;
import com.gbsoft.weather.mybatis.model.GetDustVo;
import com.gbsoft.weather.mybatis.model.GetOneCallWeatherResponse;
import com.gbsoft.weather.mybatis.model.GetOneCallWeatherVo;
import com.gbsoft.weather.mybatis.model.GetWeatherVo;
import com.gbsoft.weather.mybatis.model.GlobalLatLongVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
@EnableScheduling
public class WeatherService {

    private final WeatherMapper weatherMapper;

    private final static int[] GU_NUM_COUNT = {25, 18, 42, 22, 24, 5, 8, 5, 16, 1, 5, 10, 22, 15, 2, 16, 14};

    public GetWeatherVo getWeather(Long cityId) {

        GetWeatherVo weather = weatherMapper.getWeather(cityId)
                .orElseThrow(NoSuchCityException::new);

        weather.makeView();

        return weather;
    }

    public GetOneCallWeatherResponse getOneCallWeather(Long cityId, Long mainCityCode, Long gugunCode) {
        if (cityId < 0 ) {
            int i = getCityIdForDB(mainCityCode.intValue()) + gugunCode.intValue();
            cityId = Long.valueOf(i);
        }

        GetOneCallWeatherVo getOneCallWeatherVo = weatherMapper.getOneCallWeather(cityId)
                .orElseThrow(NoSuchCityException::new);

        return GetOneCallWeatherResponse.of(getOneCallWeatherVo);
    }

    public int getCityIdForDB(int mainCityNum) {
        int id = 0;
        if (mainCityNum != 0) {
            for (int i = 0; i < mainCityNum; i++) {
                id += GU_NUM_COUNT[i];
            }
        }
        return id;
    }

    @Transactional(readOnly = true)
    public List<Integer> getCityIdFromOpenWeather() {
        return weatherMapper.getCityIdFromOpenWeather();
    }


    public GetDustResponse getDust(int mainCityNum, int guNum, boolean grade) {

        GetDustResponse airDataFromDb = getAirDataFromDb(getCityIdForDB(mainCityNum) + guNum, grade);
        return airDataFromDb;
    }

    private GetDustResponse getAirDataFromDb(int cityId, boolean grade) {
        String pm10 = "-";
        String pm25 = "-";
        String pm10Grade = "-";
        String pm25Grade = "-";

        GetDustVo getDustVo = weatherMapper.getDust(cityId)
                .orElseThrow(NoSuchAirDataException::new);

        pm10 = getDustVo.getFineDustConcentration();
        pm25 = getDustVo.getUltrafineDustConcentration();

        log.info("pm10: " + pm10);

        if (grade) {
            if (pm10.equals("-"))
                pm10Grade = "-";
            else if (Integer.parseInt(pm10) <= 30)
                pm10Grade = "1";
            else if (Integer.parseInt(pm10) <= 80)
                pm10Grade = "2";
            else if (Integer.parseInt(pm10) <= 150)
                pm10Grade = "3";
            else
                pm10Grade = "4";

            if (pm25.equals("-"))
                pm25Grade = "-";
            else if (Integer.parseInt(pm25) <= 15)
                pm25Grade = "1";
            else if (Integer.parseInt(pm25) <= 35)
                pm25Grade = "2";
            else if (Integer.parseInt(pm25) <= 75)
                pm25Grade = "3";
            else
                pm25Grade = "4";
        }

        return GetDustResponse.builder()
                .pm10(pm10)
                .pm25(pm25)
                .pm10Grade(pm10Grade)
                .pm25Grade(pm25Grade)
                .build();
    }

    public List<CityNameVo> getCityName() {
        return weatherMapper.getCityName();
    }

	public List<GlobalLatLongVo> getGlobalLatLong() {
        return weatherMapper.getGlobalLatLong();
	}

	public int isThisMonthQueried(String thisMonth) {
        return weatherMapper.getHolidayList(thisMonth+"%");
	}
}
