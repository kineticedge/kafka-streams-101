package io.kineticedge.ks101.streams;

import com.beust.jcommander.Parameter;
import io.kineticedge.ks101.common.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
public class Options extends BaseOptions  {

    @Parameter(names = { "-g", "--application-id" }, description = "application id")
    private String applicationId = "duplicate-phones";

    @Parameter(names = { "--client-id" }, description = "client id")
    private String clientId = "s-" + UUID.randomUUID();

    @Parameter(names = { "--group-instance-id" }, description = "group instance id")
    private String groupInstanceId;

    @Parameter(names = { "--auto-offset-reset" }, description = "where to start consuming from if no offset is provided")
    private String autoOffsetReset = "earliest";

    @Parameter(names = { "--phone-alert-topic" }, description = "duplicate phones")
    private String duplicatePhonesTopic = "duplicate-phones";
}
