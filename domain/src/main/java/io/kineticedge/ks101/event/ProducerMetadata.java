package io.kineticedge.ks101.event;

import lombok.Data;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Instant;

@Data
public class ProducerMetadata {
    private String topic;
    private Integer partition;
    private Long offset;
    private Instant timestamp;

    public ProducerMetadata(RecordMetadata recordMetadata) {
        this.topic = recordMetadata.topic();
        this.partition = recordMetadata.partition();
        this.offset = recordMetadata.offset();
        this.timestamp = Instant.ofEpochMilli(recordMetadata.timestamp());
    }
}
