package searchengine.dto.exceptions;

public record ErrorDescription(boolean result, String error) {
    public ErrorDescription(String error) {
        this(false, error);
    }
    public ErrorDescription(CommonException e) {
        this(e.getError());
    }
}
