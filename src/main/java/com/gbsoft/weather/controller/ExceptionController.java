package com.gbsoft.weather.controller;

import com.gbsoft.weather.exception.ApiException;
import com.gbsoft.weather.exception.NoSuchAirDataException;
import com.gbsoft.weather.mybatis.model.ErrorVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionController {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> invalidRequestHandler(ApiException e) {

        ErrorVo errorVo = ErrorVo.builder()
                .data(e.getMessage())
                .build();

        if (e instanceof NoSuchAirDataException) {
            NoSuchAirDataException e1 = (NoSuchAirDataException) e;

            return ResponseEntity.status(HttpStatus.resolve(e1.getStatusCode()))
                    .body(e1.getBody());
        }

        return ResponseEntity.status(HttpStatus.resolve(e.getStatusCode()))
                .body(errorVo);
    }
}
