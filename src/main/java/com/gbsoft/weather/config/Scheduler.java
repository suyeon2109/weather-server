package com.gbsoft.weather.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.gbsoft.weather.mybatis.mapper.WeatherMapper;
import com.gbsoft.weather.mybatis.model.CityNameVo;
import com.gbsoft.weather.service.WeatherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Transactional
@Slf4j
public class Scheduler {
	
	@Value("${weather.MAIN_CITY_TOTAL_NUM}")
	private int mainCityTotalNum;
	@Value("${weather.GU_NUM_COUNT}")
	private int[] guNumCount;
	@Value("${weather.SHORT_CITY_NAME}")
	private String[] shortCityName;
	@Value("${weather.AIRAPI_URL}")
	private String airApiUrl;
	@Value("${weather.AIRKOREA_API_KEY}")
	private String airkoreaApiKey;

	private final WeatherService weatherService;
	private final WeatherMapper weatherMapper;
	private static List<CityNameVo> city;

	@Scheduled(cron = "${schedule.air}")
	public void timerRSSWeatherAir() throws Exception {
		city = weatherMapper.getCityName();

		for(int i=0; i<mainCityTotalNum; i++){
			getAirData(i);
			for(int j=0; j<guNumCount[i]; j++){
				getWeatherData(i, j);
			}
		}
		getGlobalAirWeather();
	}

	private void getAirData(int mainCityNum) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(airApiUrl); /*URL*/
		urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + airkoreaApiKey); /*Service Key*/
		urlBuilder.append("&" + URLEncoder.encode("returnType","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8")); /*xml 또는 json*/
		urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("125", "UTF-8")); /*한 페이지 결과 수(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지번호(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&" + URLEncoder.encode("sidoName","UTF-8") + "=" + URLEncoder.encode(shortCityName[mainCityNum], "UTF-8"));
		urlBuilder.append("&" + URLEncoder.encode("ver","UTF-8") + "=" + URLEncoder.encode("1.0", "UTF-8"));

		URL url = new URL(urlBuilder.toString());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-type", "application/json");

		BufferedReader rd;
		if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} else {
			rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
		}
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();

		int initialNum = weatherService.getCityIdForDB(mainCityNum);
		int count = guNumCount[mainCityNum];
		List<Map<String, Object>> list = parsingAirData(sb.toString(), initialNum, count);

		weatherMapper.saveAirDataToDB(list, initialNum, initialNum+count);
		log.debug("cityId {} to {} update finished", initialNum, initialNum+count-1);
	}

	private List<Map<String, Object>> parsingAirData(String data, int initialNum, int count) {
		try {
			JSONObject jObject = new JSONObject(data);
			JSONObject response = jObject.getJSONObject("response");
			JSONObject body = response.getJSONObject("body");
			JSONArray jArray = body.getJSONArray("items");

			List<Map<String,Object>> list = new ArrayList<>();

			for(int i = initialNum; i < initialNum+count; i++){
				String dustName = city.get(i).getDustName();
				int cityId = i;

				JSONObject result = StreamSupport.stream(jArray.spliterator(), false)
					.map(obj -> (JSONObject)obj)
					.filter(obj -> !obj.isNull("stationName") && dustName.equals(obj.getString("stationName")))
					.map(obj -> obj.put("cityId", cityId))
					.findFirst().orElse(null);

				if(null != result){
					Map<String, Object> dataMap = result.toMap();
					list.add(dataMap);
				}
			}
			return list;
		} catch (JSONException e) {
			log.error("========= JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			return new ArrayList<>();
		}
	}

	private void getWeatherData(int mainCityNum, int guNum) {

	}

	private void getGlobalAirWeather() {

	}

	@Scheduled(cron = "${schedule.weather}")
	public void getHourlyWeather() throws Exception {
		System.out.println("weatherData");
	}
}
