package com.gamebasic.common.exception;

import com.gamebasic.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO (Lv 10): GameNotFoundException(404)과
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundGameException(
            GameNotFoundException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    // GameFinishedException(409)을 처리하는 핸들러를 추가하세요.
    @ExceptionHandler(GameFinishedException.class)
    public ResponseEntity<ErrorResponse> handleGameFinishedException(
            GameFinishedException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "요청 값이 올바르지 않습니다."
                : fieldError.getField() + " 값이 올바르지 않습니다: " + fieldError.getDefaultMessage();
        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(
            ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("요청 값이 올바르지 않습니다.");
        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    //HttpMessageNotReadableException은 Json 자체가 잘못됐거나 타입 변환이 불가능한 경우
    //MethodArgumentTypeMismatchException은 URL 파라미터 등의 타입이 맞지 않을 때 발생
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "요청 본문이나 파라미터 형식이 올바르지 않습니다.", request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(status, message, request.getRequestURI()));
    }
}
