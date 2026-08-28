package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderFailedEvent;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    private static final String TOPIC = "order-failed";

    @Mock
    private KafkaTemplate<String, OrderFailedEvent> kafkaTemplate;

    @InjectMocks
    private OrderEventPublisher publisher;

    @Test
    @DisplayName("отправляет событие в топик order-failed с ключом = orderId")
    void publishOrderFailed_ShouldSendEventToConfiguredTopicWithOrderIdAsKey() {
        ReflectionTestUtils.setField(publisher, "orderFailedTopic", TOPIC);
        OrderFailedEvent event = new OrderFailedEvent(UUID.randomUUID(), List.of(UUID.randomUUID()), "Недостаточно товара");
        CompletableFuture<SendResult<String, OrderFailedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        publisher.publishOrderFailed(event);

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }

    @Test
    @DisplayName("при успешной отправке не выбрасывает исключение из callback'а")
    void publishOrderFailed_WhenSendSucceeds_ShouldCompleteCallbackWithoutError() {
        ReflectionTestUtils.setField(publisher, "orderFailedTopic", TOPIC);
        OrderFailedEvent event = new OrderFailedEvent(UUID.randomUUID(), List.of(UUID.randomUUID()), "reason");
        CompletableFuture<SendResult<String, OrderFailedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        ProducerRecord<String, OrderFailedEvent> producerRecord =
                new ProducerRecord<>(TOPIC, event.orderId().toString(), event);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0
        );
        SendResult<String, OrderFailedEvent> sendResult = new SendResult<>(producerRecord, recordMetadata);

        publisher.publishOrderFailed(event);
        future.complete(sendResult);

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }

    @Test
    @DisplayName("при ошибке отправки исключение из callback'а не пробрасывается наружу")
    void publishOrderFailed_WhenSendFails_ShouldSwallowErrorInCallback() {
        ReflectionTestUtils.setField(publisher, "orderFailedTopic", TOPIC);
        OrderFailedEvent event = new OrderFailedEvent(UUID.randomUUID(), List.of(UUID.randomUUID()), "reason");
        CompletableFuture<SendResult<String, OrderFailedEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(TOPIC), eq(event.orderId().toString()), eq(event))).thenReturn(future);

        publisher.publishOrderFailed(event);
        future.completeExceptionally(new RuntimeException("kafka is down"));

        verify(kafkaTemplate).send(TOPIC, event.orderId().toString(), event);
    }
}
