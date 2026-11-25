package org.ikigaidigital.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires framework-free domain/legacy classes as Spring beans, so they stay free of Spring annotations themselves. */
@Configuration(proxyBeanMethods = false)
class TimeDepositConfiguration {

    /** The legacy calculator, used as-is (README: its behaviour must remain unchanged). */
    @Bean
    TimeDepositCalculator timeDepositCalculator() {
        return new TimeDepositCalculator();
    }
}
