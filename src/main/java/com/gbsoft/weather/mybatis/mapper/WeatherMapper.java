package com.gbsoft.weather.mybatis.mapper;

import com.gbsoft.weather.dto.WeatherInfoDto;
import com.gbsoft.weather.dto.WeatherInfoRssDto;
import com.gbsoft.weather.mybatis.model.CityNameVo;
import com.gbsoft.weather.mybatis.model.GetDustVo;
import com.gbsoft.weather.mybatis.model.GetOneCallWeatherVo;
import com.gbsoft.weather.mybatis.model.GetWeatherVo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
@Repository
public interface WeatherMapper {

    Optional<GetWeatherVo> getWeather(Long cityId);

    Optional<GetOneCallWeatherVo> getOneCallWeather(Long cityId);

    Optional<GetDustVo> getDust(int cityId);

	List<CityNameVo> getCityName();

	void saveAirDataToDB(List<Map<String, Object>> list, int initialNum, int finalNum);

	void saveWeatherInfoToDB(WeatherInfoDto weatherInfoDto);

	void saveWeatherInfoRssToDB(WeatherInfoRssDto weatherInfoRssDto);
}
