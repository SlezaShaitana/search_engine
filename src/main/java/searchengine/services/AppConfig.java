package searchengine.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class AppConfig {
    @Bean
    public AtomicBoolean stopIndexingFlag() {
        return new AtomicBoolean(false);
    }
}
