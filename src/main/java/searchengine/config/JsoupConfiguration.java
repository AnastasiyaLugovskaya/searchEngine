package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jsoup")
@Getter
@Setter
public class JsoupConfiguration {
    private int timeoutMin;
    private int timeoutMax;
    private String referrer;
    private String userAgent;
}
