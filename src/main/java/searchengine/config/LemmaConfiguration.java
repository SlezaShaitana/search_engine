package searchengine.config;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LemmaConfiguration {
    @Bean
    public RussianLuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology();
    }
}
