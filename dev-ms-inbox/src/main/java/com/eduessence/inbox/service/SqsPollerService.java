package com.eduessence.inbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * Hace long-poll a SQS cada 5s. Cada mensaje en la cola es una notificación
 * SES Inbound (encapsulada en SNS) con la key del .eml en S3.
 *
 * Para desactivar: eduessence.inbox.sqs.enabled=false (perfiles locales).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eduessence.inbox.sqs.enabled", havingValue = "true", matchIfMissing = false)
public class SqsPollerService {

    private final SqsClient sqs;
    private final InboundMessageProcessor processor;

    @Value("${eduessence.inbox.sqs.queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000L)
    public void poll() {
        try {
            List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(10)
                    .build()).messages();

            for (Message m : messages) {
                try {
                    processor.procesarNotificacionSes(m.body());
                    sqs.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(m.receiptHandle())
                            .build());
                } catch (Exception ex) {
                    log.error("Error procesando mensaje SQS {}: {}", m.messageId(), ex.getMessage());
                    // No borramos → SQS lo reintenta hasta llegar al DLQ
                }
            }
        } catch (Exception ex) {
            log.error("Error consultando SQS: {}", ex.getMessage());
        }
    }
}
