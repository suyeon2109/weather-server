package com.gbsoft.weather.mybatis.mapper;

import com.gbsoft.weather.dto.GlobalWeatherDto;
import com.gbsoft.weather.dto.OpenWeatherHourlyDto;
import com.gbsoft.weather.dto.TodayMinMaxTempDto;
import com.gbsoft.weather.dto.WeatherInfoDto;
import com.gbsoft.weather.dto.WeatherInfoRssDto;
import com.gbsoft.weather.mybatis.model.CityNameVo;
import com.gbsoft.weather.mybatis.model.GetDustVo;
import com.gbsoft.weather.mybatis.model.GetOneCallWeatherVo;
import com.gbsoft.weather.mybatis.model.GetWeatherVo;
import com.gbsoft.weather.mybatis.model.GlobalLatLongVo;

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

	List<Integer> getCityIdFromOpenWeather();

	void saveOpenWeatherHourlyToDB(OpenWeatherHourlyDto openWeatherHourlyDto);

	void saveTodayMinMaxTempToDB(TodayMinMaxTempDto todayMinMaxTempDto);

	List<GlobalLatLongVo> getGlobalLatLong();

	void saveGlobalWeatherToDB(GlobalWeatherDto globalWeatherDto);

	int getHolidayList(String thisMonth);

	void saveHolidayToDb(List<Map<String, Object>> list);
}
