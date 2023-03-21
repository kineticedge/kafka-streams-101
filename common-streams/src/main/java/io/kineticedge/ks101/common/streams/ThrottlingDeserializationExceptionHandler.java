package io.kineticedge.ks101.common.streams;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

public class ThrottlingDeserializationExceptionHandler implements DeserializationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ThrottlingDeserializationExceptionHandler.class);

    private static final String REST_CLIENT_EXCEPTION_CLASS = "io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException";

    public static final String THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD = "throttling.deserialization.exception.threshold";

    //This throttling is / task.
    private double threshold = 0.0;

    @Override
    public DeserializationHandlerResponse handle(ProcessorContext context, ConsumerRecord<byte[], byte[]> record, Exception exception) {
        if (isHardFailure(exception)) {
            // regardless if threshold, fail; since it is a hard failure (system issue, configuration issue, etc).
            log("hard failure caught during deserialization", record, exception);
            return DeserializationHandlerResponse.FAIL;
        } else if (isOverThreshold(context)) {
            log("deserialization exception caught during deserialization (threshold met, stopping application)", record, exception);
            return DeserializationHandlerResponse.FAIL;
        } else {
            log("deserialization exception caught during deserialization", record, exception);
            return DeserializationHandlerResponse.CONTINUE;
        }
    }

    @Override
    public void configure(final Map<String, ?> configs) {
        if (configs == null) {
            return;
        }

        final Object value = configs.get(THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD);

        if (value == null) {
            return;
        }

        if (value instanceof Number) {
            threshold = ((Number) value).doubleValue();
        } else if (value instanceof String) {
            threshold = Double.parseDouble((String) value);
        } else {
            throw new InvalidConfigurationException("invalid type for " + THROTTLING_DESERIALIZATION_EXCEPTION_THRESHOLD);
        }
    }

    private static boolean isHardFailure(final Exception e) {
        return isUnknownHostException(e) || isHardFailureSerializationException(e);
    }

    /**
     * If Serializer is using Schema Registry and configured with an unknown host, that is a hard-failure
     * as retries would not be expected to change (at least in a short period of time), so abort immediately.
     */
    private static boolean isUnknownHostException(final Exception e) {
        return e.getCause() instanceof UnknownHostException;
    }

    /**
     * If the cause of the exception is an internal Null Pointer Exception, an HTTPS Certificate Exception, or
     * a Schema Registry RestClient exception, it is a hard-failure. This may not be complete, this is by
     * observation and conversation.
     *
     * To avoid dependency with the schema registry client, rest-client exception is checked by class-name.
     */
    private static boolean isHardFailureSerializationException(final Exception e) {
        return e instanceof SerializationException && e.getCause() != null && (e.getCause() instanceof NullPointerException
                || e.getCause() instanceof CertificateException
                || REST_CLIENT_EXCEPTION_CLASS.equals(e.getCause().getClass().getName())
        );
    }


    // do not log record's value, as it could contain PCI, PII, or other sensitive data.
    private void log(final String message, final ConsumerRecord<byte[], byte[]> record, final Exception exception) {
        log.error("{}, topic={}, partition={}, offset={}",
                message,
                record.topic(),
                record.partition(),
                record.offset(),
                exception);
    }

    // Kafka Streams 2.5 - task metric of dropped-records replaced skipped-records. Prior to Kafka Streams 2.5
    // this was a thread metric (skipped-records-rate).
    //
    // MBean: kafka.streams:type=stream-task-metrics,thread-id=[threadId],task-id=[taskId]
    //
    private MetricName metricName(final ProcessorContext context) {
        final Map<String, String> tags = new HashMap<>();
        tags.put("thread-id", Thread.currentThread().getName());
        tags.put("task-id", context.taskId().toString());
        // group, name, & tags used for lookup (description ignored; not part of equals)
        return new MetricName("dropped-records-rate", "stream-task-metrics", "", tags);
    }

    private double metricValue(final Metric metric) {
        return ((Number) metric.metricValue()).doubleValue();
    }

    private Metric metric(final ProcessorContext context) {
        return context.metrics().metrics().get(metricName(context));
    }

    private boolean isOverThreshold(final ProcessorContext context) {
        final Metric metric = metric(context);
        if (metric != null) {
            double skipRate = metricValue(metric);
            log.debug("checking threshold : skipRate={}, threshold={}", skipRate, threshold);
            return skipRate > threshold;
        } else {
            log.error("unable to lookup `dropped-records-rate` metric, check to see if metric has changed.");
            return true;
        }
    }
}