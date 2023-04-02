package io.kineticedge.ks101.admin;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static io.kineticedge.ks101.common.util.JsonUtil.objectMapper;

public class TopicOffsets {

    @Getter
    @Setter
    public static class Options {

        @Parameter(names = { "--since" }, required = true, converter = DateTimeConverter.class)
        private ZonedDateTime since;

        @Parameter(names = { "--topic" }, required = true)
        private String topic;

    }

    public static void main(String[] args) throws Exception {

        TopicOffsets.Options topicOffsetOptions = new TopicOffsets.Options();

        JCommander jCommander = JCommander.newBuilder()
                .addObject(topicOffsetOptions)
                .build();
        jCommander.parse(args);

        System.out.println(topicOffsetOptions.since);

        io.kineticedge.ks101.admin.Options options = new io.kineticedge.ks101.admin.Options();

        Admin admin = new Admin(options);


        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> since = admin.offsets(topicOffsetOptions.getTopic(), OffsetSpec.forTimestamp(topicOffsetOptions.getSince().toInstant().toEpochMilli()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> now = admin.offsets(topicOffsetOptions.getTopic(), OffsetSpec.latest());


        System.out.printf("Number of messages since %s is: %s\n", topicOffsetOptions.getSince(), diff(since, now));
    }


    private static Long diff(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> start, Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> end) {

        Map<TopicPartition, Long> diff = end.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), offset(start.get(e.getKey()), e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return diff.values().stream().reduce(0L, Long::sum);
    }

    private static long offset(final ListOffsetsResult.ListOffsetsResultInfo start, final ListOffsetsResult.ListOffsetsResultInfo end) {

        if (start.offset() == -1 && end.offset() == -1) {
            return 0L;
        } else if (start.offset() == -1 && end.offset() >= 0) {
            // no messages have been written to this partition since the timestamp given.
            // could mean partition has no data, or could mean "since" is in the future.
            return 0L;
        } else if (end.offset() == -1) {
            System.out.println("timestamp provided is beyond offset we have.");
            return 0L;
        } else {
            return end.offset() - start.offset();
        }
    }

    private static String format(Object object) {
        try {
            return objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object) + "\n";
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
