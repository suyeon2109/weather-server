package com.gbsoft.weather.scheduler;

import static com.slack.api.webhook.WebhookPayloads.*;
import static java.nio.charset.StandardCharsets.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

import com.gbsoft.weather.dto.GlobalWeatherDto;
import com.gbsoft.weather.dto.GlobalWeatherParsingResult;
import com.gbsoft.weather.dto.OpenWeatherHourlyDto;
import com.gbsoft.weather.dto.ShortWeatherParsingResult;
import com.gbsoft.weather.dto.TodayMinMaxTempDto;
import com.gbsoft.weather.dto.WeatherInfoDto;
import com.gbsoft.weather.dto.WeatherInfoRssDto;
import com.gbsoft.weather.mybatis.mapper.WeatherMapper;
import com.gbsoft.weather.mybatis.model.CityNameVo;
import com.gbsoft.weather.mybatis.model.GlobalLatLongVo;
import com.gbsoft.weather.service.WeatherService;
import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;

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
	@Value("${weather.CURRENT_URL}")
	private String currentUrl;
	@Value("${weather.WEATHER_API_KEY}")
	private String weatherApiKey;

	@Value("${weather.GLOBALWEATHER_API_KEY}")
	private String globalWeatherApiKey;
	@Value("${weather.GLOBALWEATHER_URL1}")
	private String globalWeatherUrl1;
	@Value("${weather.GLOBALWEATHER_URL2}")
	private String globalWeatherUrl2;
	@Value("${weather.GLOBALAIR_URL1}")
	private String globalAirUrl1;

	@Value("${weather.HOLIDAY_API_URL}")
	private String holidayApiUrl;
	@Value("${weather.HOLIDAY_API_KEY}")
	private String holidayApiKey;
	@Value("${weather.WEBHOOK_URL}")
	private String webhookUrl;



	private final WeatherService weatherService;
	private final WeatherMapper weatherMapper;
	private List<CityNameVo> city;

	@Scheduled(cron = "${schedule.air}")
	public void timerRSSWeatherAir() throws Exception {
		city = weatherService.getCityName();
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
		urlBuilder.append("?serviceKey=").append(airkoreaApiKey); /*Service Key*/
		urlBuilder.append("&returnType=").append(URLEncoder.encode("json", UTF_8)); /*xml 또는 json*/
		urlBuilder.append("&numOfRows=").append(URLEncoder.encode("125", UTF_8)); /*한 페이지 결과 수(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&pageNo=").append(URLEncoder.encode("1", UTF_8)); /*페이지번호(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&sidoName=").append(URLEncoder.encode(shortCityName[mainCityNum], UTF_8));
		urlBuilder.append("&ver=").append(URLEncoder.encode("1.0", UTF_8));

		String response = requestUrl(urlBuilder);

		int initialNum = weatherService.getCityIdForDB(mainCityNum);
		int count = guNumCount[mainCityNum];
		List<Map<String, Object>> list = parseAirData(response, initialNum, count);

		if (!list.isEmpty()) {
			weatherMapper.saveAirDataToDB(list, initialNum, initialNum+count);
			log.debug("AirQualityInfo - cityId {} to {} update finished", initialNum, initialNum+count-1);
			// log.info("AirQualityInfo - cityId {} to {} update finished", initialNum, initialNum+count-1);
		}
	}

	private String requestUrl(StringBuilder urlBuilder) throws IOException {
		URL url = new URL(urlBuilder.toString());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();

		try {
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-type", "application/json");

			BufferedReader rd;
			if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
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
		} catch (SocketException e) {
			conn.disconnect();
			log.error("========= SocketException ==========");
			log.error(e.getMessage());
			return "";
		}
	}

	private List<Map<String, Object>> parseAirData(String data, int initialNum, int count) throws IOException {
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
			log.error("========= AirData JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			sendSlackMessage("AirData JSON Parsing error", data);
			return new ArrayList<>();
		}
	}

	private void sendSlackMessage(String title, String message) throws IOException {
		Slack slack = Slack.getInstance();
		slack.send(webhookUrl, payload(p -> p
			.attachments(
				List.of(generateSlackAttachment(title, message))
			)
		));
	}

	private Attachment generateSlackAttachment(String title, String message) {
		return Attachment.builder()
			.color("ff0000") // 빨간색
			.title(title)
			.fields(List.of(Field.builder()
					.title("Error message")
					.value(message)
					.valueShortEnough(false)
					.build()
				)
			)
			.build();
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
			// log.info("WeatherInfo, WeatherInfoRss - cityId {} update finished", initialNum+guNum);
		}
	}



	private String getShortWeatherResponse(int initialNum, int guNum) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(rssUrlShort); /*URL*/
		urlBuilder.append(URLEncoder.encode(city.get(initialNum + guNum).getRss_short(),UTF_8)); /* zone */

		return requestUrl(urlBuilder);
	}

	private String getLongWeatherResponse(int mainCityNum) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(rssUrlLong); /*URL*/
		urlBuilder.append(URLEncoder.encode(String.valueOf(rssLongCode[mainCityNum]),UTF_8)); /* zone */

		return requestUrl(urlBuilder);
	}

	private ShortWeatherParsingResult parseShortWeather(String data, int cityId) throws IOException {
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
			log.error("========= ShortWeather(WeatherInfo) JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			sendSlackMessage("ShortWeather(WeatherInfo) JSON Parsing error", data);
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

	@Scheduled(cron = "${schedule.weather}")
	public void getHourlyWeather() throws IOException {
		city = weatherService.getCityName();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		int hour = now.getHour();

		List<Integer> cityIdList = weatherService.getCityIdFromOpenWeather();
		for (Integer cityId : cityIdList) {
			saveTodayWeatherData(cityId);

			if (hour == 0) {
				getTodayMinMaxTemp(cityId);
			}
		}
	}

	private void saveTodayWeatherData(Integer cityId) throws IOException {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		int hour = now.getHour();
		StringBuilder currentUrl = getCurrentUrl(cityId, now);

		String response = requestUrl(currentUrl);

		OpenWeatherHourlyDto openWeatherHourlyDto = parseCurrentWeather(cityId, response, hour);

		if(null != openWeatherHourlyDto.getCurrent()){
			weatherMapper.saveOpenWeatherHourlyToDB(openWeatherHourlyDto);
			log.debug("OpenWeatherHourly - cityId {} update finished", cityId);
			// log.info("OpenWeatherHourly - cityId {} update finished", cityId);
		}
	}

	private StringBuilder getCurrentUrl(int cityId, LocalDateTime nowDateTime) {
		LocalDateTime date = nowDateTime;
		LocalDate nowDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
		int hour = date.getHour();
		String baseTime = "";
		
		if (hour < 3) {
			if (nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(2,30,0)))) {
				date = date.minusDays(1L);
				baseTime = "2300";
			} else {
				baseTime = "0200";
			}
		} else if (hour < 6) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(5, 20, 0))) ? "0200" : "0500";
		} else if (hour < 9) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(8, 20, 0))) ? "0500" : "0800";
		} else if (hour < 12) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(11, 20, 0))) ? "0800" : "1100";
		} else if (hour < 15) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(14, 20, 0))) ? "1100" : "1400";
		} else if (hour < 18) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(17, 20, 0))) ? "1400" : "1700";
		} else if (hour < 21) {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(20, 20, 0))) ? "1700" : "2000";
		} else {
			baseTime = nowDateTime.isBefore(LocalDateTime.of(nowDate, LocalTime.of(23, 20, 0))) ? "2000" : "2300";
		}

		StringBuilder urlBuilder = new StringBuilder(currentUrl); /*URL*/
		urlBuilder.append(weatherApiKey); /*Service Key*/
		urlBuilder.append("&pageNo=").append(URLEncoder.encode("1", UTF_8)); /*xml 또는 json*/
		urlBuilder.append("&numOfRows=").append(URLEncoder.encode("165", UTF_8)); /*한 페이지 결과 수(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&dataType=").append(URLEncoder.encode("json", UTF_8)); /*페이지번호(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&base_date=").append(URLEncoder.encode(String.format("%d", date.getYear()) + String.format("%02d", date.getMonthValue()) + String.format("%02d", date.getDayOfMonth()), UTF_8));
		urlBuilder.append("&base_time=").append(URLEncoder.encode(baseTime, UTF_8));
		urlBuilder.append("&nx=").append(URLEncoder.encode(String.valueOf(city.get(cityId).getX()), UTF_8));
		urlBuilder.append("&ny=").append(URLEncoder.encode(String.valueOf(city.get(cityId).getY()), UTF_8));

		return urlBuilder;
	}


	private OpenWeatherHourlyDto parseCurrentWeather(Integer cityId, String data, int hour) throws IOException {
		try {
			JSONObject jObject = new JSONObject(data);
			JSONObject response = jObject.getJSONObject("response");
			JSONObject header = response.getJSONObject("header");
			JSONObject body = response.getJSONObject("body");
			JSONObject items = body.getJSONObject("items");

			if(!"00".equals(header.getString("resultCode"))) {
				throw new JSONException("Hourly data is none");
			}

			JSONArray jArray = items.getJSONArray("item");

			Map<String, String> map = new HashMap<>();
			Map<String, Map<String, String>> hourlyMap = new HashMap<>();
			String hourlyMapKey = "";
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject obj = (JSONObject)jArray.get(i);
				if(!obj.isNull("category") && !obj.isNull("fcstValue") && !obj.isNull("fcstTime")) {
					if(!hourlyMapKey.equals(obj.getString("fcstTime"))) map = new HashMap<>();

					map.put(obj.getString("category"), obj.getString("fcstValue"));
					hourlyMap.put(obj.getString("fcstTime"), map);
					hourlyMapKey = obj.getString("fcstTime");
				}
			}

			JSONObject jsonObject = StreamSupport.stream(jArray.spliterator(), false)
				.map(JSONObject.class::cast)
				.filter(obj -> !obj.isNull("fcstTime") && (String.format("%02d", hour) + "00").equals(obj.getString("fcstTime")))
				.findFirst().orElse(null);

			int currentHour = hour;

			if(null == jsonObject) {
				currentHour += 1;
				if(currentHour == 24) {
					currentHour = 0;
				}
			}

			String current = makeDataString(hourlyMap, currentHour);
			String hour1 = makeDataString(hourlyMap, (currentHour + 1) >= 24 ? currentHour + 1 - 24 : currentHour + 1);
			String hour2 = makeDataString(hourlyMap, (currentHour + 2) >= 24 ? currentHour + 2 - 24 : currentHour + 2);
			String hour3 = makeDataString(hourlyMap, (currentHour + 3) >= 24 ? currentHour + 3 - 24 : currentHour + 3);
			String hour4 = makeDataString(hourlyMap, (currentHour + 4) >= 24 ? currentHour + 4 - 24 : currentHour + 4);
			String hour5 = makeDataString(hourlyMap, (currentHour + 5) >= 24 ? currentHour + 5 - 24 : currentHour + 5);
			String hour6 = makeDataString(hourlyMap, (currentHour + 6) >= 24 ? currentHour + 6 - 24 : currentHour + 6);

			return OpenWeatherHourlyDto.builder()
				.cityId(cityId)
				.current(current)
				.hour1(hour1)
				.hour2(hour2)
				.hour3(hour3)
				.hour4(hour4)
				.hour5(hour5)
				.hour6(hour6)
				.updateTime(LocalDateTime.now().toString().replaceAll("-","").replaceAll("T", " ").substring(0,17))
				.build();
		} catch (JSONException e) {
			log.error("========= CurrentWeather(OpenWeatherHourly) JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			sendSlackMessage("CurrentWeather(OpenWeatherHourly) JSON Parsing error", data);
			return OpenWeatherHourlyDto.builder().build();
		}
	}

	private String makeDataString(Map<String, Map<String, String>> hourlyMap, int hour) {
		String dataString = hourlyMap.get(String.format("%02d", hour)+"00").get("TMP") + ";"
			+ hourlyMap.get(String.format("%02d", hour)+"00").get("POP") + ";"
			+ hourlyMap.get(String.format("%02d", hour)+"00").get("REH") + ";"
			+ hourlyMap.get(String.format("%02d", hour)+"00").get("WSD") + ";"
			// 0 : N, 1 : NE, 2 : E, 3 : SE, 4 : S, 5 : SW, 6 : W, 7 : NW, 8 : N
			+ (int)((Integer.parseInt(hourlyMap.get(String.format("%02d", hour)+"00").get("VEC")) + 22.5) / 45) + ";"
			+ getSkyConditionNum(Integer.parseInt(hourlyMap.get(String.format("%02d", hour)+"00").get("SKY")), Integer.parseInt(
			hourlyMap.get(String.format("%02d", hour)+"00").get("PTY"))) + ";";
		return dataString;
	}

	private String getSkyConditionNum(int sky, int pty) {
		// 0 : 맑음
		// 1 : 흐림
		// 2 : 구름많음
		// 3 : 흐리고 비
		// 4 : 비/눈
		// 5 : 비
		// 6 : 눈
		// 8 : 소나기
		// - 하늘상태(SKY) 코드 : 맑음(1), 구름많음(3), 흐림(4)
		// - 강수형태(PTY) 코드 : (단기) 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)

		if (pty == 1) {
			if(sky == 4) return String.valueOf(3);
			else return String.valueOf(5);
		} else if (pty == 2) {
			return String.valueOf(4);
		} else if (pty == 3) {
			return String.valueOf(6);
		} else if (pty == 4) {
			return String.valueOf(8);
		}

		if (sky == 1) {
			return String.valueOf(0);
		} else if (sky == 3) {
			return String.valueOf(2);
		} else if (sky == 4) {
			return String.valueOf(1);
		} else {
			return String.valueOf(-1);
		}
	}

	private void getTodayMinMaxTemp(Integer cityId) throws IOException {
		StringBuilder yesterdayUrl = getYesterdayUrl(cityId);

		String response = requestUrl(yesterdayUrl);
		TodayMinMaxTempDto todayMinMaxTempDto = parseTodayMinMaxTemp(cityId, response);

		if(null != todayMinMaxTempDto.getMaxTemp()){
			weatherMapper.saveTodayMinMaxTempToDB(todayMinMaxTempDto);
			log.debug("TodayMinMaxTemp - cityId {} update finished", cityId);
			// log.info("TodayMinMaxTemp - cityId {} update finished", cityId);
		}
	}
	private StringBuilder getYesterdayUrl(Integer cityId) {
		LocalDateTime yesterday = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(1L);

		StringBuilder urlBuilder = new StringBuilder(currentUrl); /*URL*/
		urlBuilder.append(weatherApiKey); /*Service Key*/
		urlBuilder.append("&pageNo=").append(URLEncoder.encode("1", UTF_8)); /*xml 또는 json*/
		urlBuilder.append("&numOfRows=").append(URLEncoder.encode("300", UTF_8)); /*한 페이지 결과 수(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&dataType=").append(URLEncoder.encode("json", UTF_8)); /*페이지번호(조회 날짜로 검색 시 사용 안함)*/
		urlBuilder.append("&base_date=").append(URLEncoder.encode(String.format("%d", yesterday.getYear()) + String.format("%02d", yesterday.getMonthValue()) + String.format("%02d", yesterday.getDayOfMonth()), UTF_8));
		urlBuilder.append("&base_time=").append(URLEncoder.encode("2300", UTF_8));
		urlBuilder.append("&nx=").append(URLEncoder.encode(String.valueOf(city.get(cityId).getX()), UTF_8));
		urlBuilder.append("&ny=").append(URLEncoder.encode(String.valueOf(city.get(cityId).getY()), UTF_8));

		return urlBuilder;
	}

	private TodayMinMaxTempDto parseTodayMinMaxTemp(Integer cityId, String data) throws IOException {
		try {
			JSONObject jObject = new JSONObject(data);
			JSONObject response = jObject.getJSONObject("response");
			JSONObject header = response.getJSONObject("header");
			JSONObject body = response.getJSONObject("body");
			JSONObject items = body.getJSONObject("items");

			if(!"00".equals(header.getString("resultCode"))) {
				throw new JSONException("ParseTodayMinMaxTemp resultCode is not 00");
			}

			JSONArray jArray = items.getJSONArray("item");
			LocalDateTime todayDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
			String todayDate = String.format("%d", todayDateTime.getYear()) + String.format("%02d", todayDateTime.getMonthValue()) + String.format("%02d", todayDateTime.getDayOfMonth());
			String maxTemp = "";
			String minTemp = "";

			List<JSONObject> result = StreamSupport.stream(jArray.spliterator(), false)
				.map(JSONObject.class::cast)
				.filter(obj -> !obj.isNull("fcstDate") && todayDate.equals(obj.getString("fcstDate")))
				.collect(Collectors.toList());

			for (JSONObject obj : result) {
				if(!obj.isNull("category") && "TMX".equals(obj.getString("category"))) {
					maxTemp = obj.getString("fcstValue");
				} else if (!obj.isNull("category") && "TMN".equals(obj.getString("category"))) {
					minTemp = obj.getString("fcstValue");
				}
			}

			return TodayMinMaxTempDto.builder()
				.cityId(cityId)
				.date(todayDate)
				.maxTemp(maxTemp)
				.minTemp(minTemp)
				.updateTime(todayDateTime.toString().replaceAll("-", "").replaceAll("T", " ").substring(0, 17))
				.build();
		} catch (JSONException e) {
			log.error("========= TodayMinMaxTemp JSON Parsing error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			sendSlackMessage("TodayMinMaxTemp JSON Parsing error", data);
			return TodayMinMaxTempDto.builder().build();
		}
	}

	private void getGlobalAirWeather() throws IOException {
		List<GlobalLatLongVo> globalLatLong = weatherService.getGlobalLatLong();

		for (GlobalLatLongVo g : globalLatLong) {
			StringBuilder weatherUrl = new StringBuilder(globalWeatherUrl1);
			weatherUrl.append("lat=").append(URLEncoder.encode(String.valueOf(g.getLatitude()), UTF_8));
			weatherUrl.append("&lon=").append(URLEncoder.encode(String.valueOf(g.getLongitude()), UTF_8));
			weatherUrl.append(globalWeatherUrl2);
			weatherUrl.append(globalWeatherApiKey);

			StringBuilder airUrl = new StringBuilder(globalAirUrl1);
			airUrl.append("lat=").append(URLEncoder.encode(String.valueOf(g.getLatitude()), UTF_8));
			airUrl.append("&lon=").append(URLEncoder.encode(String.valueOf(g.getLongitude()), UTF_8));
			airUrl.append("&appid=").append(globalWeatherApiKey);

			String weatherResponse = requestUrl(weatherUrl);
			String airResponse = requestUrl(airUrl);

			GlobalWeatherParsingResult globalWeatherResult = parseWeatherResponse(weatherResponse, g.getUtc());
			JSONObject globalAirResult = parseAirResponse(airResponse);

			if (!globalWeatherResult.getWeek().isEmpty() && !globalAirResult.isEmpty()) {
				GlobalWeatherDto globalWeatherDto = makeGlobalWeatherDto(globalWeatherResult, globalAirResult, g);
				weatherMapper.saveGlobalWeatherToDB(globalWeatherDto);

				log.debug("Global Weather - Id {} update finished", g.getId());
				// log.info("Global Weather - Id {} update finished", g.getId());
			}
		}
	}

	private GlobalWeatherParsingResult parseWeatherResponse(String weatherResponse, int utc) throws IOException {
		try {
			JSONObject weatherObj = new JSONObject(weatherResponse);
			JSONObject current = weatherObj.getJSONObject("current");

			Long currentTemp = Math.round(current.getDouble("temp") - 273.15);
			int currentHumidity = Math.round((float)current.getInt("humidity") /10)*10;
			JSONObject currentWeather = (JSONObject)current.getJSONArray("weather").get(0);
			String currentMain = currentWeather.getString("main");
			String currentDesc = currentWeather.getString("description");
			String currentIcon = currentWeather.getString("icon");

			JSONArray hourly = weatherObj.getJSONArray("hourly");
			JSONArray daily = weatherObj.getJSONArray("daily");

			List<JSONObject> hour = new ArrayList();

			hour.add((JSONObject)hourly.get(2));
			hour.add((JSONObject)hourly.get(5));
			hour.add((JSONObject)hourly.get(8));

			int today = 0;
			String weatherStr = "";
			Map<String, String> weather = new HashMap<>();
			List<JSONObject> week = new ArrayList();
			for (int i = 0; i < 24; i++) {
				JSONObject obj = (JSONObject)hourly.get(i);
				ZonedDateTime utcDt = Instant.ofEpochSecond(obj.getLong("dt"))
					.atZone(ZoneId.of("UTC"));
				ZonedDateTime utcNow = utcDt.plusHours(utc);

				if (i == 0) {
					today = utcNow.getDayOfWeek().getValue() - 1;
				} else if (i < 8) { // 1 < i < 8
					JSONObject dailyObj = (JSONObject)daily.get(i);
					week.add(dailyObj);
				}

				int time = utcNow.getHour();
				if (time == 0 || time == 3 || time == 6 || time == 9 || time == 12 || time == 15 || time == 18
					|| time == 21) {
					weatherStr = String.valueOf(Math.round(obj.getDouble("temp") - 273.15));
					weatherStr += ";" + Math.round((float)obj.getInt("humidity") / 10) * 10;
					weatherStr += ";" + ((JSONObject)obj.getJSONArray("weather").get(0)).getString("main");
					weatherStr += ";" + ((JSONObject)obj.getJSONArray("weather").get(0)).getString("icon");
					weather.put(String.valueOf(time), weatherStr);
				}
			}

			return GlobalWeatherParsingResult.builder()
				.today(today)
				.week(week)
				.currentTemp(currentTemp)
				.currentHumidity(currentHumidity)
				.currentMain(currentMain)
				.currentDesc(currentDesc)
				.currentIcon(currentIcon)
				.hour(hour)
				.weather(weather).build();
		} catch (JSONException e) {
			log.error("========= GlobalWeather JSON Parsing error ==========");
			log.error("WeatherData = {}", weatherResponse);
			log.error(e.getMessage());
			sendSlackMessage("GlobalWeather JSON Parsing error", weatherResponse);
			return GlobalWeatherParsingResult.builder().build();
		}
	}

	private JSONObject parseAirResponse(String airResponse) throws IOException {
		try{
			JSONObject airObj = new JSONObject(airResponse);
			JSONArray list = airObj.getJSONArray("list");
			JSONObject pmObj = ((JSONObject)list.get(0)).getJSONObject("components");
			return pmObj;
		} catch (JSONException e) {
			log.error("========= GlobalAirData JSON Parsing error ==========");
			log.error("AirData = {}", airResponse);
			log.error(e.getMessage());
			sendSlackMessage("GlobalAirData JSON Parsing error", airResponse);
			return new JSONObject();
		}
	}

	private GlobalWeatherDto makeGlobalWeatherDto(GlobalWeatherParsingResult weatherResult, JSONObject airResult, GlobalLatLongVo globalLatLongVo) {
		return GlobalWeatherDto.builder()
			.id(globalLatLongVo.getId())
			.updateTimeKr(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString().replaceAll("T", "/").substring(0,16))
			.countryCode(globalLatLongVo.getCountryCode())
			.cityCode(globalLatLongVo.getCityCode())
			.pm25(Math.round(airResult.getDouble("pm2_5")))
			.pm10(Math.round(airResult.getDouble("pm10")))
			.today(weatherResult.getToday())
			.minTemp(Math.round(((JSONObject)weatherResult.getWeek().get(0).get("temp")).getDouble("min") - 273.15))
			.maxTemp(Math.round(((JSONObject)weatherResult.getWeek().get(0).get("temp")).getDouble("max") - 273.15))
			.currentTemp(weatherResult.getCurrentTemp())
			.currentMain(weatherResult.getCurrentMain())
			.currentDesc(weatherResult.getCurrentDesc())
			.currentIcon(weatherResult.getCurrentIcon())

			.hour1Time(Instant.ofEpochSecond(weatherResult.getHour().get(0).getLong("dt"))
				.atZone(ZoneId.of("UTC")).plusHours(globalLatLongVo.getUtc()).getHour())
			.hour1Temp(Math.round(weatherResult.getHour().get(0).getDouble("temp") - 273.15))
			.hour1Icon(((JSONObject)weatherResult.getHour().get(0).getJSONArray("weather").get(0)).getString("icon"))

			.hour2Time(Instant.ofEpochSecond(weatherResult.getHour().get(1).getLong("dt"))
				.atZone(ZoneId.of("UTC")).plusHours(globalLatLongVo.getUtc()).getHour())
			.hour2Temp(Math.round(weatherResult.getHour().get(1).getDouble("temp") - 273.15))
			.hour2Icon(((JSONObject)weatherResult.getHour().get(1).getJSONArray("weather").get(0)).getString("icon"))

			.hour3Time(Instant.ofEpochSecond(weatherResult.getHour().get(2).getLong("dt"))
				.atZone(ZoneId.of("UTC")).plusHours(globalLatLongVo.getUtc()).getHour())
			.hour3Temp(Math.round(weatherResult.getHour().get(2).getDouble("temp") - 273.15))
			.hour3Icon(((JSONObject)weatherResult.getHour().get(2).getJSONArray("weather").get(0)).getString("icon"))

			.week1TMax(Math.round(weatherResult.getWeek().get(0).getJSONObject("temp").getDouble("max") - 273.15))
			.week1TMin(Math.round(weatherResult.getWeek().get(0).getJSONObject("temp").getDouble("min") - 273.15))
			.week1Desc(((JSONObject)weatherResult.getWeek().get(0).getJSONArray("weather").get(0)).getString("description"))
			.week1Icon(((JSONObject)weatherResult.getWeek().get(0).getJSONArray("weather").get(0)).getString("icon"))

			.week2TMax(Math.round(weatherResult.getWeek().get(1).getJSONObject("temp").getDouble("max") - 273.15))
			.week2TMin(Math.round(weatherResult.getWeek().get(1).getJSONObject("temp").getDouble("min") - 273.15))
			.week2Desc(((JSONObject)weatherResult.getWeek().get(1).getJSONArray("weather").get(0)).getString("description"))
			.week2Icon(((JSONObject)weatherResult.getWeek().get(1).getJSONArray("weather").get(0)).getString("icon"))

			.week3TMax(Math.round(weatherResult.getWeek().get(2).getJSONObject("temp").getDouble("max") - 273.15))
			.week3TMin(Math.round(weatherResult.getWeek().get(2).getJSONObject("temp").getDouble("min") - 273.15))
			.week3Desc(((JSONObject)weatherResult.getWeek().get(2).getJSONArray("weather").get(0)).getString("description"))
			.week3Icon(((JSONObject)weatherResult.getWeek().get(2).getJSONArray("weather").get(0)).getString("icon"))

			.week4TMax(Math.round(weatherResult.getWeek().get(3).getJSONObject("temp").getDouble("max") - 273.15))
			.week4TMin(Math.round(weatherResult.getWeek().get(3).getJSONObject("temp").getDouble("min") - 273.15))
			.week4Desc(((JSONObject)weatherResult.getWeek().get(3).getJSONArray("weather").get(0)).getString("description"))
			.week4Icon(((JSONObject)weatherResult.getWeek().get(3).getJSONArray("weather").get(0)).getString("icon"))

			.week5TMax(Math.round(weatherResult.getWeek().get(4).getJSONObject("temp").getDouble("max") - 273.15))
			.week5TMin(Math.round(weatherResult.getWeek().get(4).getJSONObject("temp").getDouble("min") - 273.15))
			.week5Desc(((JSONObject)weatherResult.getWeek().get(4).getJSONArray("weather").get(0)).getString("description"))
			.week5Icon(((JSONObject)weatherResult.getWeek().get(4).getJSONArray("weather").get(0)).getString("icon"))

			.week6TMax(Math.round(weatherResult.getWeek().get(5).getJSONObject("temp").getDouble("max") - 273.15))
			.week6TMin(Math.round(weatherResult.getWeek().get(5).getJSONObject("temp").getDouble("min") - 273.15))
			.week6Desc(((JSONObject)weatherResult.getWeek().get(5).getJSONArray("weather").get(0)).getString("description"))
			.week6Icon(((JSONObject)weatherResult.getWeek().get(5).getJSONArray("weather").get(0)).getString("icon"))

			.week7TMax(Math.round(weatherResult.getWeek().get(6).getJSONObject("temp").getDouble("max") - 273.15))
			.week7TMin(Math.round(weatherResult.getWeek().get(6).getJSONObject("temp").getDouble("min") - 273.15))
			.week7Desc(((JSONObject)weatherResult.getWeek().get(6).getJSONArray("weather").get(0)).getString("description"))
			.week7Icon(((JSONObject)weatherResult.getWeek().get(6).getJSONArray("weather").get(0)).getString("icon"))

			.currentHumidity(weatherResult.getCurrentHumidity())
			.valueOf12am(weatherResult.getWeather().get("0"))
			.valueOf3am(weatherResult.getWeather().get("3"))
			.valueOf6am(weatherResult.getWeather().get("6"))
			.valueOf9am(weatherResult.getWeather().get("9"))
			.valueOf12pm(weatherResult.getWeather().get("12"))
			.valueOf3pm(weatherResult.getWeather().get("15"))
			.valueOf6pm(weatherResult.getWeather().get("18"))
			.valueOf9pm(weatherResult.getWeather().get("21"))

			.week1(Math.round(((JSONObject)weatherResult.getWeek().get(0).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(0).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(0).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(0).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(0).getJSONArray("weather").get(0)).getString("icon"))
			.week2(Math.round(((JSONObject)weatherResult.getWeek().get(1).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(1).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(1).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(1).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(1).getJSONArray("weather").get(0)).getString("icon"))
			.week3(Math.round(((JSONObject)weatherResult.getWeek().get(2).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(2).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(2).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(2).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(2).getJSONArray("weather").get(0)).getString("icon"))
			.week4(Math.round(((JSONObject)weatherResult.getWeek().get(3).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(3).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(3).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(3).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(3).getJSONArray("weather").get(0)).getString("icon"))
			.week5(Math.round(((JSONObject)weatherResult.getWeek().get(4).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(4).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(4).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(4).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(4).getJSONArray("weather").get(0)).getString("icon"))
			.week6(Math.round(((JSONObject)weatherResult.getWeek().get(5).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(5).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(5).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(5).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(5).getJSONArray("weather").get(0)).getString("icon"))
			.week7(Math.round(((JSONObject)weatherResult.getWeek().get(6).get("temp")).getDouble("max") - 273.15)
				+ ";" + Math.round(((JSONObject)weatherResult.getWeek().get(6).get("temp")).getDouble("min") - 273.15)
				+ ";" + Math.round(weatherResult.getWeek().get(6).getInt("humidity") /10)*10
				+ ";" + ((JSONObject)weatherResult.getWeek().get(6).getJSONArray("weather").get(0)).getString("main")
				+ ";" + ((JSONObject)weatherResult.getWeek().get(6).getJSONArray("weather").get(0)).getString("icon"))
			.build();
	}

	@Scheduled(cron = "${schedule.holiday}")
	public void getMonthlyHoliday() throws IOException {
		log.info("schedule.holiday");

		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		String thisMonth = String.format("%d", now.getYear()) +"-"+ String.format("%02d", now.getMonthValue());

		int holidayCount = weatherService.isThisMonthQueried(thisMonth);

		if(holidayCount == 0) {
			getMonthlyHoliday(now.getYear(), now.getMonthValue());
		}
	}

	private void getMonthlyHoliday(int year, int month) throws IOException {
		StringBuilder urlBuilder = new StringBuilder(holidayApiUrl);
		urlBuilder.append("serviceKey=").append(holidayApiKey);
		urlBuilder.append("&solYear=").append(URLEncoder.encode(String.format("%d", year), UTF_8));
		urlBuilder.append("&solMonth=").append(URLEncoder.encode(String.format("%02d", month), UTF_8));
		urlBuilder.append("&_type=").append(URLEncoder.encode("json", UTF_8));

		String response = requestUrl(urlBuilder);
		List<Map<String, Object>> resultList = parseHolidayData(response, year, month);

		if (!resultList.isEmpty()) {
			weatherMapper.saveHolidayToDb(resultList);

			log.debug("MonthlyHoliday {}-{} update finished", year, String.format("%02d",month));
			// log.info("MonthlyHoliday {}-{} update finished", year, String.format("%02d",month));
		}
	}

	private List<Map<String, Object>> parseHolidayData(String data, int year, int month) throws IOException {
		try {
			JSONObject jObject = new JSONObject(data);
			JSONObject response = jObject.getJSONObject("response");
			JSONObject header = response.getJSONObject("header");
			JSONObject body = response.getJSONObject("body");
			int totalCount = body.getInt("totalCount");

			if (!"00".equals(header.getString("resultCode"))) {
				throw new JSONException("Holiday data is none");
			}

			List<Map<String, Object>> resultList = new ArrayList<>();
			Map<String, Object> resultMap = new HashMap<>();
			if (totalCount == 0) {
				resultMap.put("dateName", "no holiday");
				resultMap.put("isHoliday", "Y");
				resultMap.put("locdate", String.format("%d", year) + String.format("%02d", month) + "00");

				resultList.add(resultMap);
			} else {
				JSONObject items = body.getJSONObject("items");
				JSONArray jArray = items.getJSONArray("item");

				List<Map<String, Object>> collect = StreamSupport.stream(jArray.spliterator(), false)
					.map(JSONObject.class::cast)
					.filter(obj -> !obj.isNull("dateName") && !obj.isNull("isHoliday") && !obj.isNull("locdate") && "Y".equals(obj.getString("isHoliday")))
					.map(JSONObject::toMap)
					.collect(Collectors.toList());
				resultList = collect;
			}

			return resultList;
		} catch (JSONException e) {
			log.error("========= getMonthlyHoliday : error ==========");
			log.error("Data = {}", data);
			log.error(e.getMessage());
			sendSlackMessage("getMonthlyHoliday JSON Parsing error", data);
			return new ArrayList<>();
		}
	}
}
