package com.iara.common.crypto;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Ponte entre o container de instanciacao do JPA/Hibernate (que cria
 * AttributeConverters via reflexao, FORA do ciclo de vida do Spring) e o
 * contexto de beans do Spring.
 *
 * Motivo de existir: {@link jakarta.persistence.AttributeConverter} e
 * instanciado pelo provider JPA (Hibernate), nao pelo Spring — portanto nao
 * suporta @Autowired construtor. Para que {@link EncryptedStringConverter}
 * consiga usar o bean {@link ColumnEncryptor} (que le a chave de criptografia
 * validada uma unica vez no boot), ele busca o bean manualmente atraves
 * deste holder estatico, que e populado quando o Spring inicializa este
 * componente.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException(
                "ApplicationContext ainda nao foi inicializado. " +
                "EncryptedStringConverter nao pode ser usado antes do boot completo do Spring.");
        }
        return context.getBean(beanClass);
    }
}
