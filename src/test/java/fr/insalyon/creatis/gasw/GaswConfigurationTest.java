package fr.insalyon.creatis.gasw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class GaswConfigurationTest {

    @Test
    @DisplayName("all declared property keys resolve against gasw.properties")
    void allPropertyKeys_resolveAgainstGaswProperties() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(PlaceholderConfig.class, GaswConfiguration.class);
            assertDoesNotThrow(ctx::refresh,
                    "a @Value key in GaswConfiguration has no matching entry in gasw.properties");
        }
    }

    @Configuration
    static class PlaceholderConfig {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
