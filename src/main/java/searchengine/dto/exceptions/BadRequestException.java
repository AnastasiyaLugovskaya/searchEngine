package searchengine.dto.exceptions;

public class BadRequestException extends CommonException{
    public BadRequestException(String error) {
        super(error);
    }
}
