package com.mz.example.api;

import com.mz.example.service.exception.JobNotFoundException;
import com.mz.example.service.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
//NOTE: exceptions caught by this handler are not logged by default.
public class APIErrorHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Throwable throwable){
        log.error("Unexpected server error", throwable);
        return produceErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error. Contact system administrator for details.");
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(JobNotFoundException ex){
        return produceErrorResponse(HttpStatus.NOT_FOUND, "No such job");
    }

    private ResponseEntity<ErrorResponse> produceErrorResponse(HttpStatus status, String msg){
        return ResponseEntity.status(status).body(new ErrorResponse(status, msg));
    }
}
