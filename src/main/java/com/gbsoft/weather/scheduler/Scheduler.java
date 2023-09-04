package com.gbsoft.weather.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.gbsoft.weather.dto.WeatherInfoDto;
import com.gbsoft.weather.dto.ShortWeatherParsingResult;
import com.gbsoft.weather.dto.WeatherInfoRssDto;
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
	@Value("${weather.RSS_URL_SHORT}")
	private String rssUrlShort;
	@Value("${weather.RSS_URL_LONG}")
	private String rssUrlLong;
	@Value("${weather.RSS_LONG_CODE}")
	private int[] rssLongCode;


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

		String response = requestUrl(urlBuilder);

		int initialNum = weatherService.getCityIdForDB(mainCityNum);
		int count = guNumCount[mainCityNum];
		List<Map<String, Object>> list = parseAirData(response, initialNum, count);

		weatherMapper.saveAirDataToDB(list, initialNum, initialNum+count);
		log.debug("AirQualityInfo - cityId {} to {} update finished", initialNum, initialNum+count-1);
	}

	private String requestUrl(StringBuilder urlBuilder) throws IOException {
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
		return sb.toString();
	}

	private List<Map<String, Object>> parseAirData(String data, int initialNum, int count) {
		try {
			JSONObject jObject = new JSONObject(data);
			JSONObject response = jObject.getJSONObject("response");
			JSONObject body = response.getJSONObject("body");
			JSONArray jArray = body.getJSONArray("items");

			List<Map<String,Object>> list = new ArrayList<>();

			for(int i = initialNum; i < initialNum+count; i++){
				String dustName = city.get(i).getDustName();
				int cityId = i;

				JSONObject shortResult = StreamSupport.stream(jArray.spliterator(), false)
					.map(obj -> (JSONObject)obj)
					.filter(obj -> !obj.isNull("stationName") && dustName.equals(obj.getString("stationName")))
					.map(obj -> obj.put("cityId", cityId))
					.findFirst().orElse(null);

				if(null != shortResult){
					Map<String, Object> dataMap = shortResult.toMap();
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

	private void getWeatherData(int mainCityNum, int guNum) throws IOException {
		int initialNum = weatherService.getCityIdForDB(mainCityNum);
		String shortWeatherResponse = getShortWeatherResponse(initialNum, guNum);
		String longWeatherResponse = getLongWeatherResponse(mainCityNum);

		ShortWeatherParsingResult shortWeatherParsingResult = parseShortWeather(shortWeatherResponse, initialNum+guNum);
		List<Map<String, Object>> weekWeather2 = parseLongWeather(longWeatherResponse, initialNum, mainCityNum, guNum);

		if(weekWeather2.isEmpty()){
			Map<String, Object> map = new HashMap<>();
			map.put("tmn", "-");
			map.put("tmx", "-");
			map.put("rnSt", "-");
			map.put("wf", "-");

			weekWeather2.add(map);
			weekWeather2.add(map);
			weekWeather2.add(map);
			weekWeather2.add(map);
			weekWeather2.add(map);
		}


		if(null != shortWeatherParsingResult.getTodayWeather()){
			WeatherInfoDto weatherInfoDto = makeWeatherInfoDto(shortWeatherParsingResult, weekWeather2);
			WeatherInfoRssDto weatherInfoRssDto = makeWeatherInfoRssDto(shortWeatherParsingResult);
			weatherMapper.saveWeatherInfoToDB(weatherInfoDto);
			weatherMapper.saveWeatherInfoRssToDB(weatherInfoRssDto);

			log.debug("WeatherInfo, WeatherInfoRss - cityId {} update finished", initialNum+guNum);
		}
	}



	private String getShortWeatherResponse(int initialNum, int guNum) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(rssUrlShort); /*URL*/
		urlBuilder.append(URLEncoder.encode(city.get(initialNum + guNum).getRss_short(),"UTF-8")); /* zone */

		return requestUrl(urlBuilder);
	}

	private String getLongWeatherResponse(int mainCityNum) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(rssUrlLong); /*URL*/
		urlBuilder.append(URLEncoder.encode(String.valueOf(rssLongCode[mainCityNum]),"UTF-8")); /* zone */

		return requestUrl(urlBuilder);
	}

	private ShortWeatherParsingResult parseShortWeather(String data, int cityId) {
		try {
			JSONObject jObject = XML.toJSONObject(data);
			JSONObject body = jObject.getJSONObject("rss").getJSONObject("channel").getJSONObject("item").getJSONObject("description").getJSONObject("body");
			JSONArray jArray = body.getJSONArray("data");

			// todayWeather
			JSONObject firstObj = (JSONObject)jArray.get(0);
			Map<String, Object> todayWeather = firstObj.toMap();

			List<Map<String, Object>> hourWeather = new ArrayList<>();
			List<Map<String, Object>> weekWeather1 = new ArrayList<>();
			Map<Integer, Map> every3hWeather = new HashMap<>();

			for(int i = 0; i < jArray.length(); i++) {
				JSONObject obj = (JSONObject)jArray.get(i);
				Map<String, Object> map = obj.toMap();
				if((Integer) map.get("hour") == 12) {
					if((Integer) map.get("day") == 1 || (Integer) map.get("day") == 2) {
						// weekWeather1
						weekWeather1.add(map);
					}
				}

				if(i < 8){
					// every3hWeather
					every3hWeather.put((Integer) map.get("hour"), map);

					if(i < 4) {
						// hourWeather
						hourWeather.add(map);
					}
				}
			}

			return ShortWeatherParsingResult.builder()
				.todayWeather(todayWeather)
				.hourWeather(hourWeather)
				.weekWeather1(weekWeather1)
				.every3hWeather(every3hWeather)
				.date(LocalDate.now().toString().replaceAll("-", ""))
				.updateDate(LocalDateTime.now().toString().replaceAll("-","").replaceAll("T", " ").substring(0,17))
				.cityId(cityId)
				.build();
		} catch (JSONException e) {
			log.error("========= JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			return ShortWeatherParsingResult.builder().build();
		}
	}

	private List<Map<String, Object>> parseLongWeather(String data, int initialNum, int mainCityNum, int guNum) {
		JSONObject jObject = XML.toJSONObject(data);
		JSONObject body = jObject.getJSONObject("rss").getJSONObject("channel").getJSONObject("item").getJSONObject("description").getJSONObject("body");
		JSONArray jArray = body.getJSONArray("location");

		int[] cityNum = {0, 5, 6, 7, 8, 9, 10, 11};
		String searchCityName = Arrays.stream(cityNum).anyMatch(i -> i == mainCityNum) ? shortCityName[mainCityNum] : city.get(initialNum+guNum).getWeatherName();

		List<Map<String, Object>> weekWeather2 = new ArrayList<>();
		List<JSONArray> list = StreamSupport.stream(jArray.spliterator(), false)
			.map(JSONObject.class::cast)
			.filter(obj -> !obj.isNull("city") && searchCityName.contains(obj.getString("city").substring(0, 2)))
			.map(obj -> obj.getJSONArray("data"))
			.collect(Collectors.toList());

		for(JSONArray arr : list) {
			for (int i = 0; i < 10; i++) {
				JSONObject jsonObject = (JSONObject)arr.get(i);
				Map<String, Object> cityWeatherMap = jsonObject.toMap();
				if(cityWeatherMap.get("tmEf").toString().contains("12:00")){
					weekWeather2.add(cityWeatherMap);
				}
			}
		}

		return weekWeather2;
	}

	private WeatherInfoDto makeWeatherInfoDto(ShortWeatherParsingResult shortResult, List<Map<String, Object>> weekWeather2) {
		return WeatherInfoDto.builder()
			.date(shortResult.getDate())
			.temperature((double)shortResult.getTodayWeather().get("temp"))
			.weather((String)shortResult.getTodayWeather().get("wfKor"))
			.precipitationProbability((Integer)shortResult.getTodayWeather().get("pop"))
			.windSpeed(shortResult.getTodayWeather().get("ws").toString().length() > 4 ?
				String.format("%.2f", shortResult.getTodayWeather().get("ws"))
				: shortResult.getTodayWeather().get("ws").toString())
			.windDirection((String)shortResult.getTodayWeather().get("wdKor"))
			.humidity((Integer)shortResult.getTodayWeather().get("reh"))
			.hourT1((Integer)shortResult.getHourWeather().get(0).get("hour"))
			.hourTH1(String.format("%.1f", shortResult.getHourWeather().get(0).get("temp")))
			.hourK1((String)shortResult.getHourWeather().get(0).get("wfKor"))
			.hourT2((Integer)shortResult.getHourWeather().get(1).get("hour"))
			.hourTH2(String.format("%.1f", shortResult.getHourWeather().get(1).get("temp")))
			.hourK2((String)shortResult.getHourWeather().get(1).get("wfKor"))
			.hourT3((Integer)shortResult.getHourWeather().get(2).get("hour"))
			.hourTH3(String.format("%.1f", shortResult.getHourWeather().get(2).get("temp")))
			.hourK3((String)shortResult.getHourWeather().get(2).get("wfKor"))

			.weekT1((String.format("%.1f", shortResult.getWeekWeather1().get(0).get("tmn"))))
			.weekTH1((String.format("%.1f", shortResult.getWeekWeather1().get(0).get("tmx"))))
			.weekPop1((Integer)shortResult.getWeekWeather1().get(0).get("pop"))
			.weekK1((String)shortResult.getWeekWeather1().get(0).get("wfKor"))

			.weekT2((String.format("%.1f", shortResult.getWeekWeather1().get(1).get("tmn"))))
			.weekTH2((String.format("%.1f", shortResult.getWeekWeather1().get(1).get("tmx"))))
			.weekPop2((Integer)shortResult.getWeekWeather1().get(1).get("pop"))
			.weekK2((String)shortResult.getWeekWeather1().get(1).get("wfKor"))

			.weekT3(String.valueOf(weekWeather2.get(0).get("tmn")))
			.weekTH3(String.valueOf(weekWeather2.get(0).get("tmx")))
			.weekPop3(String.valueOf(weekWeather2.get(0).get("rnSt")))
			.weekK3((String)weekWeather2.get(0).get("wf"))

			.weekT4(String.valueOf(weekWeather2.get(1).get("tmn")))
			.weekTH4(String.valueOf(weekWeather2.get(1).get("tmx")))
			.weekPop4(String.valueOf(weekWeather2.get(1).get("rnSt")))
			.weekK4((String)weekWeather2.get(1).get("wf"))

			.weekT5(String.valueOf(weekWeather2.get(2).get("tmn")))
			.weekTH5(String.valueOf(weekWeather2.get(2).get("tmx")))
			.weekPop5(String.valueOf(weekWeather2.get(2).get("rnSt")))
			.weekK5((String)weekWeather2.get(2).get("wf"))

			.weekT6(String.valueOf(weekWeather2.get(3).get("tmn")))
			.weekTH6(String.valueOf(weekWeather2.get(3).get("tmx")))
			.weekPop6(String.valueOf(weekWeather2.get(3).get("rnSt")))
			.weekK6((String)weekWeather2.get(3).get("wf"))

			.weekT7(String.valueOf(weekWeather2.get(4).get("tmn")))
			.weekTH7(String.valueOf(weekWeather2.get(4).get("tmx")))
			.weekPop7(String.valueOf(weekWeather2.get(4).get("rnSt")))
			.weekK7((String)weekWeather2.get(4).get("wf"))
			.updateTime(shortResult.getUpdateDate())
			.cityId(shortResult.getCityId())
			.build();
	}

	private WeatherInfoRssDto makeWeatherInfoRssDto(ShortWeatherParsingResult result) {
		Map<Integer, Map> every3hWeather = result.getEvery3hWeather();

		return WeatherInfoRssDto.builder()
			.date(result.getDate())
			.temperature((double)result.getTodayWeather().get("temp"))
			.precipitationProbability((Integer)result.getTodayWeather().get("pop"))
			.humidity((Integer)result.getTodayWeather().get("reh"))
			.windSpeed(result.getTodayWeather().get("ws").toString().length() > 4 ?
				String.format("%.2f", result.getTodayWeather().get("ws"))
				: result.getTodayWeather().get("ws").toString())
			.windDirection((String)result.getTodayWeather().get("wdKor"))
			.weather((String)result.getTodayWeather().get("wfKor"))
			.hour24(every3hWeather.get(24).get("temp")+";"+every3hWeather.get(24).get("pop")+";"+every3hWeather.get(24).get("reh")+";"+ (every3hWeather.get(24).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(24).get("ws")) : every3hWeather.get(24).get("ws").toString())+";"+every3hWeather.get(24).get("wdKor")+";"+every3hWeather.get(24).get("wfKor")+";")
			.hour03(every3hWeather.get(3).get("temp")+";"+every3hWeather.get(3).get("pop")+";"+every3hWeather.get(3).get("reh")+";"+(every3hWeather.get(3).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(3).get("ws")) : every3hWeather.get(3).get("ws").toString())+";"+every3hWeather.get(3).get("wdKor")+";"+every3hWeather.get(3).get("wfKor")+";")
			.hour06(every3hWeather.get(6).get("temp")+";"+every3hWeather.get(6).get("pop")+";"+every3hWeather.get(6).get("reh")+";"+(every3hWeather.get(6).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(6).get("ws")) : every3hWeather.get(6).get("ws").toString())+";"+every3hWeather.get(6).get("wdKor")+";"+every3hWeather.get(6).get("wfKor")+";")
			.hour09(every3hWeather.get(9).get("temp")+";"+every3hWeather.get(9).get("pop")+";"+every3hWeather.get(9).get("reh")+";"+(every3hWeather.get(9).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(9).get("ws")) : every3hWeather.get(9).get("ws").toString())+";"+every3hWeather.get(9).get("wdKor")+";"+every3hWeather.get(9).get("wfKor")+";")
			.hour12(every3hWeather.get(12).get("temp")+";"+every3hWeather.get(12).get("pop")+";"+every3hWeather.get(12).get("reh")+";"+(every3hWeather.get(12).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(12).get("ws")) : every3hWeather.get(12).get("ws").toString())+";"+every3hWeather.get(12).get("wdKor")+";"+every3hWeather.get(12).get("wfKor")+";")
			.hour15(every3hWeather.get(15).get("temp")+";"+every3hWeather.get(15).get("pop")+";"+every3hWeather.get(15).get("reh")+";"+(every3hWeather.get(15).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(15).get("ws")) : every3hWeather.get(15).get("ws").toString())+";"+every3hWeather.get(15).get("wdKor")+";"+every3hWeather.get(15).get("wfKor")+";")
			.hour18(every3hWeather.get(18).get("temp")+";"+every3hWeather.get(18).get("pop")+";"+every3hWeather.get(18).get("reh")+";"+(every3hWeather.get(18).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(18).get("ws")) : every3hWeather.get(18).get("ws").toString())+";"+every3hWeather.get(18).get("wdKor")+";"+every3hWeather.get(18).get("wfKor")+";")
			.hour21(every3hWeather.get(21).get("temp")+";"+every3hWeather.get(21).get("pop")+";"+every3hWeather.get(21).get("reh")+";"+(every3hWeather.get(21).get("ws").toString().length() > 4 ? String.format("%.2f", every3hWeather.get(21).get("ws")) : every3hWeather.get(21).get("ws").toString())+";"+every3hWeather.get(21).get("wdKor")+";"+every3hWeather.get(21).get("wfKor")+";")
			.updateTime(result.getUpdateDate())
			.cityId(result.getCityId())
			.build();
	}


	private void getGlobalAirWeather() {

	}

	@Scheduled(cron = "${schedule.weather}")
	public void getHourlyWeather() throws Exception {
		System.out.println("weatherData");
	}
}
