package br.dev.guisleri.mototrack.exception;

import br.dev.guisleri.mototrack.dto.ApiErrorResponseDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_REQUEST_BODY_MESSAGE =
            "O corpo da requisição contém valores inválidos ou mal formatados.";
    private static final String VALIDATION_FAILED_MESSAGE =
            "A requisição contém campos inválidos.";

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
    public ResponseEntity<ApiErrorResponseDTO> handleMethodArgumentNotValid(
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

        ApiErrorResponseDTO validationErrorResponse =
                new ApiErrorResponseDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        VALIDATION_FAILED_MESSAGE,
                        errors
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validationErrorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "O parâmetro '%s' contém um valor inválido."
                        .formatted(exception.getName())
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMissingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "O parâmetro '%s' é obrigatório."
                        .formatted(exception.getParameterName())
        );
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleNoHandlerFound(Exception exception) {
        return createErrorResponse(HttpStatus.NOT_FOUND, "Endpoint não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {
        return createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não permitido para este endpoint.",
                exception.getHeaders()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception
    ) {
        return createErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteúdo não suportado.",
                exception.getHeaders()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception
    ) {
        return createErrorResponse(
                HttpStatus.NOT_ACCEPTABLE,
                "Formato de resposta solicitado não é suportado.",
                exception.getHeaders()
        );
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
        return createErrorResponse(status, message, new HttpHeaders());
    }

    private ResponseEntity<ApiErrorResponseDTO> createErrorResponse(
            HttpStatus status,
            String message,
            HttpHeaders headers
    ) {
        ApiErrorResponseDTO errorResponse = new ApiErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of()
        );

        return ResponseEntity
                .status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

}
