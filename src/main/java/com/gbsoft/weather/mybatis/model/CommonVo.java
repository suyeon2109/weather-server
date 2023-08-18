package com.gbsoft.weather.mybatis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Getter
@Builder
public class CommonVo {
    private Object resultCode;
    private String message;

    @JsonInclude(value = NON_EMPTY)
    private HashMap<String, Object> payload;
}
