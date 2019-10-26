package com.mz.example.service.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Getter
public class ErrorResponse {

    private ZonedDateTime timestamp;
    private int status;
    private String message;

    public ErrorResponse(HttpStatus httpStatus, String msg){
        status = httpStatus.value();
        message = msg;
        timestamp = ZonedDateTime.now();
    }
}
