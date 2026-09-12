package com.iara.common.config;

import com.iara.common.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao de entrada do modulo iara-common.
 *
 * Modulos consumidores (iara-auth, iara-cycle, etc.) devem importar este
 * modulo como dependencia Maven; o component scan do proprio modulo que
 * declarar {@code @SpringBootApplication} no pacote com.iara.* ja alcanca
 * as classes abaixo automaticamente. Esta classe existe para tornar essa
 * dependencia explicita e para registrar {@link JwtProperties} como
 * @ConfigurationProperties tipada.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = "com.iara.common")
public class CommonAutoConfiguration {
}
