package com.gbsoft.weather.controller;

import java.util.Properties;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gbsoft.weather.scheduler.Scheduler;
import com.gbsoft.weather.dto.AuthCheckDto;
import com.gbsoft.weather.service.WeatherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@CrossOrigin(origins = {"*"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class WeatherController {
	private static final String SCHEDULED_TASKS = "scheduledTasks";

	private final ScheduledAnnotationBeanPostProcessor postProcessor;

	private final Scheduler scheduler;

	@PostMapping("/startWeatherParsing")
	public ResponseEntity weatherParsing(@RequestBody AuthCheckDto authCheckDto) throws Exception {
		if("GB_WEATHER_SERVER_TIMER_START_REQUEST".equals(authCheckDto.getAuth())){
			Properties properties = System.getProperties();
			// properties.put("schedule.air", "0 0 */2 * * *");	// 2시간 마다
			// properties.put("schedule.weather", "0 0 * * * *");	// 1시간 마다
			// properties.put("schedule.holiday", "0 0 0 * * *");	// 하루 한번 0시에

			properties.put("schedule.air", "0 */5 * * * *");
			properties.put("schedule.weather", "0 */5 * * * *");
			properties.put("schedule.holiday", "0 */5 * * * *");

			postProcessor.postProcessAfterInitialization(scheduler, SCHEDULED_TASKS);

			return ResponseEntity.status(HttpStatus.OK).body("scheduler 구동 완료");
		} else {
			postProcessor.postProcessBeforeDestruction(scheduler, SCHEDULED_TASKS);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

}
