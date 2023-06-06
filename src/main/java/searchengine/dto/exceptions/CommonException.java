package searchengine.dto.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CommonException extends RuntimeException{
    private final String error;
}
