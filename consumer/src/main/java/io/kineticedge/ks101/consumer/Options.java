package io.kineticedge.ks101.consumer;

import com.beust.jcommander.Parameter;
import io.kineticedge.ks101.common.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.UUID;

@Getter
@Setter
public class Options extends BaseOptions {

    @Parameter(names = { "--poll-duration" }, description = "poll duration")
    private Duration pollDuration = Duration.ofMillis(500L);

    @Parameter(names = { "--client-id" }, description = "client id")
    private String clientId = "s-" + UUID.randomUUID();

    @Parameter(names = { "--group-instance-id" }, description = "group instance id")
    private String groupInstanceId;

    @Parameter(names = { "--auto-offset-reset" }, description = "where to start consuming from if no offset is provided")
    private String autoOffsetReset = "earliest";


}
