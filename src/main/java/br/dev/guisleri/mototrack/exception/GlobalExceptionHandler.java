package br.dev.guisleri.mototrack.exception;

import br.dev.guisleri.mototrack.dto.ApiErrorResponseDTO;
import br.dev.guisleri.mototrack.dto.ValidationErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_REQUEST_BODY_MESSAGE =
            "O corpo da requisição contém valores inválidos ou mal formatados.";

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleTripNotFound(
            TripNotFoundException exception
    ) {
        return createErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidTripDateException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInvalidTripDate(
            InvalidTripDateException exception
    ) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(InvalidTripStatusException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInvalidTripStatus(
            InvalidTripStatusException exception
    ) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(MotorcycleNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMotorcycleNotFound(
            MotorcycleNotFoundException exception
    ) {
        return createErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(MotorcycleInUseException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMotorcycleInUse(
            MotorcycleInUseException exception
    ) {
        return createErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUserNotFound(
            UserNotFoundException exception
    ) {
        return createErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUserAlreadyExists(
            UserAlreadyExistsException exception
    ) {
        return createErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(MotorcycleAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMotorcycleAccessDenied(
            MotorcycleAccessDeniedException exception
    ) {
        return createErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(TripAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleTripAccessDenied(
            TripAccessDeniedException exception
    ) {
        return createErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST_BODY_MESSAGE
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ValidationErrorResponseDTO validationErrorResponse =
                new ValidationErrorResponseDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        errors
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(validationErrorResponse);
    }

    private ResponseEntity<ApiErrorResponseDTO> createErrorResponse(
            HttpStatus status,
            RuntimeException exception
    ) {
        return createErrorResponse(status, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponseDTO> createErrorResponse(
            HttpStatus status,
            String message
    ) {
        ApiErrorResponseDTO errorResponse = new ApiErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }

}
