package searchengine.dto.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandler extends ResponseEntityExceptionHandler {
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDescription> handler(Exception e) {
        if (e instanceof NotFoundException nfe) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDescription(nfe));
        }
        if (e instanceof BadRequestException bre) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDescription(bre));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDescription(
                "Запрос не может быть выполнен в следствие внутренней ошибки на сервере"));
    }
}
