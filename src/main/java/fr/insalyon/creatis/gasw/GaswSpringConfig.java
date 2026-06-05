package fr.insalyon.creatis.gasw;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan("fr.insalyon.creatis.gasw")
@EnableTransactionManagement
@EnableScheduling
public class GaswSpringConfig {
}