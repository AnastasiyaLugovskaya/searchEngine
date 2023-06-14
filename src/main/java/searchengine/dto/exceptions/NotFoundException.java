package searchengine.dto.exceptions;

public class NotFoundException extends CommonException {
    public NotFoundException(String error) {
        super(error);
    }
}
