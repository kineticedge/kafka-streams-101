package io.kineticedge.ks101.common.config;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseOptions {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    @Parameter(names = { "-b", "--bootstrap-servers" }, description = "cluster bootstrap servers")
    private String bootstrapServers = "localhost:19092,localhost:29092,localhost:39092";

    @Parameter(names = { "--names-topic" }, description = "names topic")
    private String namesTopics = "names";

    @Parameter(names = { "--email-topic" }, description = "email topic")
    private String emailTopic = "emails";

    @Parameter(names = { "--phone-topic" }, description = "phone topic")
    private String phoneTopic = "phones";

    @Parameter(names = { "--customer360-topic" }, description = "customer360 topic")
    private String customer360Topic = "customers360";

}
