package br.com.vidaconecta.shared.web;

import br.com.vidaconecta.shared.api.ApiError;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
	public ResponseEntity<ApiError> forbidden(RuntimeException exception, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> conflict(ConflictException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> unauthorized(AuthenticationException exception, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<String> details = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		return ResponseEntity.badRequest().body(ApiError.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Requisição inválida",
				request.getRequestURI(),
				details));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> constraint(ConstraintViolationException exception, HttpServletRequest request) {
		List<String> details = exception.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.toList();
		return ResponseEntity.badRequest().body(ApiError.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Requisição inválida",
				request.getRequestURI(),
				details));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiError> business(BusinessException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ApiError.of(
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI()));
	}
}
