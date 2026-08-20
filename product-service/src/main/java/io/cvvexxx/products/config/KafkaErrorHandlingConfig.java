package io.cvvexxx.products.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Подхватывается автоконфигурацией Spring Boot и применяется ко всем @KafkaListener
     * в этом сервисе. При непойманном исключении в слушателе: до 4 повторов с экспоненциальной
     * задержкой, а если и это не помогло — запись публикуется в топик "<topic>.DLT" вместо
     * того, чтобы тихо потеряться после логирования.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    /**
     * Явно заводим DLT-топик, чтобы не полагаться на auto.create.topics.enable брокера
     * (в проде он обычно выключен). Этот сервис слушает order-created, поэтому его DLT — order-created.DLT.
     */
    @Bean
    public NewTopic orderCreatedDeadLetterTopic(@Value("${app.kafka.topics.order-created}") String topic) {
        return TopicBuilder.name(topic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
