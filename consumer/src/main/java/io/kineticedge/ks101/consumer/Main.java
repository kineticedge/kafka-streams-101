package io.kineticedge.ks101.consumer;

import io.kineticedge.ks101.common.config.OptionsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    final static Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> System.err.println("Uncaught exception in thread '" + t.getName() + "': " + e.getMessage());

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        final UpdateConsumer updateConsumer = new UpdateConsumer(options);
        final Customer360Consumer consumer = new Customer360Consumer(options);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.close();
            updateConsumer.close();
        }));

        final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });

        executor.submit(() -> {
            consumer.consume();
        });

        executor.submit(() -> {
            updateConsumer.consume();
        });
    }

}

