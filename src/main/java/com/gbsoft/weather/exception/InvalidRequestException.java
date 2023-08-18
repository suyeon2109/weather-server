package com.gbsoft.weather.exception;

import lombok.Getter;

@Getter
public class InvalidRequestException extends ApiException {

    private static final String message = "Weather Server Error(Invalid Request, Internal Error)";

    public InvalidRequestException() {
        super(message);
        //addValidation("cause", "고객아이디 오류 횟수를 초과하였습니다." + System.lineSeparator()  + "고객센터에 요청하여 이용 제한을 해제해 주시기 바랍니다.");
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }

}
