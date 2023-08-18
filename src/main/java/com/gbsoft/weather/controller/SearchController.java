package com.gbsoft.weather.controller;

import com.gbsoft.weather.exception.InvalidRequestException;
import com.gbsoft.weather.mybatis.model.GetDustResponse;
import com.gbsoft.weather.mybatis.model.GetOneCallWeatherResponse;
import com.gbsoft.weather.mybatis.model.GetWeatherVo;
import com.gbsoft.weather.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin(origins = {"https://emplite.gb-on.co.kr", "https://lite.gb-on.co.kr", "localhost:3000"})
@CrossOrigin(origins = {"*"})
@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/getWeather")
    public ResponseEntity<GetWeatherVo> getWeather(@RequestParam("cityId") Long cityId) {
        validate(cityId);
        GetWeatherVo weather = searchService.getWeather(cityId);

        return ResponseEntity.ok()
                .body(weather);
    }

    private void validate(Long cityId) {
        if (cityId < 0) {
            throw new InvalidRequestException();
        }
    }

    @GetMapping("/getOneCallWeather")
    public ResponseEntity<GetOneCallWeatherResponse> getOneCallWeather(@RequestParam(value = "cityId", required = false) Long cityId
                                                                ,@RequestParam(value = "mainCityCode", required = false) Long mainCityCode
                                                                ,@RequestParam(value = "gugunCode", required = false) Long gugunCode) {
        if (cityId == null) {
            cityId = -1l;
        }

        if (mainCityCode == null) {
            mainCityCode = -1l;
        }

        if (gugunCode == null) {
            gugunCode = -1l;
        }

        validate2(cityId, mainCityCode, gugunCode);
        GetOneCallWeatherResponse oneCallWeather = searchService.getOneCallWeather(cityId, mainCityCode, gugunCode);

        return ResponseEntity.ok()
                .body(oneCallWeather);
    }

    private void validate2(Long cityId, Long mainCityCode, Long gugunCode) {

        if (cityId == null && mainCityCode == null && gugunCode == null) {
            throw new InvalidRequestException();
        }



        if (cityId < 0 && mainCityCode < 0 && gugunCode < 0) {
            throw new InvalidRequestException();
        }
    }


    @PostMapping("/getDust")
    public ResponseEntity<GetDustResponse> getDust(@RequestBody getDustRequestDto requestDto) {

        validate3(requestDto.getMainCity(), requestDto.getGugun());

        int mainCityNum = requestDto.getMainCity();
        int gugunNum    = requestDto.getGugun();

        GetDustResponse res = searchService.getDust(mainCityNum, gugunNum, true);
        return ResponseEntity.ok()
                .body(res);
    }

    private void validate3(Integer mainCity, Integer gugun) {
        log.info("mainCity: {}", mainCity);
        log.info("gugun: {}", gugun);
        if (mainCity == null || gugun == null || mainCity < 0 || gugun < 0)  {
            throw new InvalidRequestException();
        }
    }
}
