package pe.edu.flores.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pe.edu.flores.service.BusinessException;
@RestControllerAdvice
public class ErrorAdvice {
 @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<?> resource(){return ResponseEntity.status(404).body(Map.of("message","Recurso no encontrado"));}
 @ExceptionHandler(BusinessException.class) ResponseEntity<?> business(BusinessException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<?> missing(){return ResponseEntity.status(404).body(Map.of("message","Registro no encontrado"));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> integrity(){return ResponseEntity.status(409).body(Map.of("message","El registro está duplicado o incumple una relación de datos. Revise los campos e inténtelo nuevamente."));}
 @ExceptionHandler({MethodArgumentNotValidException.class,IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Revise los datos y formatos de la solicitud"));}
 @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) ResponseEntity<?> denied(){return ResponseEntity.status(403).body(Map.of("message","Su rol no tiene permiso para esta operación"));}
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<?> size(){return ResponseEntity.badRequest().body(Map.of("message","El archivo supera el tamaño máximo de 5 MB"));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).error("Error interno",e);return ResponseEntity.status(500).body(Map.of("message","No se pudo completar la operación. Consulte el registro del servidor."));}
}
