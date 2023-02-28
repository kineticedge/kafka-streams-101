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

    @Parameter(names = { "--phone-alert-topic" }, description = "duplicate phones")
    private String duplicatePhonesTopic = "duplicate-phones";
}
