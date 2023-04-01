package io.kineticedge.ks101.common.streams;

import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ShutdownHook implements Runnable {

    private static final Duration SHUTDOWN = Duration.ofSeconds(30);

    private final Logger log = LoggerFactory.getLogger(ShutdownHook.class);

    private final KafkaStreams streams;

    public ShutdownHook(final KafkaStreams streams) {
        this.streams = streams;
    }

    @Override
    public void run() {

        log.info("Runtime shutdown hook, state={}", streams.state());
        if (streams.state().isRunningOrRebalancing()) {

            // New to Kafka Streams 3.3, you can have the application leave the group on shutting down (when member.id / static membership is used).
            //
            // There are reasons to do this and not to do it; from a development standpoint this makes starting/stopping
            // the application a lot easier reducing the time needed to rejoin the group.
            boolean leaveGroup = true;

            log.info("closing KafkaStreams with leaveGroup={}", leaveGroup);

            // can I interrupt the thread....

            KafkaStreams.CloseOptions closeOptions = new KafkaStreams.CloseOptions().timeout(SHUTDOWN).leaveGroup(leaveGroup);

            boolean isClean = streams.close(closeOptions);
            if (!isClean) {
                System.out.println("KafkaStreams was not closed cleanly");
            }

        } else if (streams.state().isShuttingDown()) {
            log.info("Kafka Streams is already shutting down with state={}, will wait {} to ensure proper shutdown.", streams.state(), SHUTDOWN);
            boolean isClean = streams.close(SHUTDOWN);
            if (!isClean) {
                System.out.println("KafkaStreams was not closed cleanly");
            }
            System.out.println("final KafkaStreams state=" + streams.state());
        }
    }
}
