package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOrderEventPublisherTest {

    private static final String TOPIC = "order-created";

    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @InjectMocks
    private DefaultOrderEventPublisher publisher;

    @Test
    @DisplayName("отправляет событие в топик order-created с ключом = orderId")
    void publishOrderCreated_ShouldSendEventToConfiguredTopicWithOrderIdAsKey() {
        ReflectionTestUtils.setField(publisher, "orderCreatedTopic", TOPIC);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), new BigDecimal("100.00"), List.of());
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        publisher.publishOrderCreated(event);

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }

    @Test
    @DisplayName("при успешной отправке не выбрасывает исключение из callback'а")
    void publishOrderCreated_WhenSendSucceeds_ShouldCompleteCallbackWithoutError() {
        ReflectionTestUtils.setField(publisher, "orderCreatedTopic", TOPIC);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), new BigDecimal("100.00"), List.of());
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        ProducerRecord<String, OrderCreatedEvent> producerRecord =
                new ProducerRecord<>(TOPIC, event.orderId().toString(), event);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0
        );
        SendResult<String, OrderCreatedEvent> sendResult = new SendResult<>(producerRecord, recordMetadata);

        publisher.publishOrderCreated(event);
        future.complete(sendResult);

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }

    @Test
    @DisplayName("при ошибке отправки исключение из callback'а не пробрасывается наружу")
    void publishOrderCreated_WhenSendFails_ShouldSwallowErrorInCallback() {
        ReflectionTestUtils.setField(publisher, "orderCreatedTopic", TOPIC);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), new BigDecimal("100.00"), List.of());
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        publisher.publishOrderCreated(event);
        future.completeExceptionally(new RuntimeException("kafka is down"));

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }
}
