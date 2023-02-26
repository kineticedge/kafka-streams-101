package io.kineticedge.ks101.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kineticedge.ks101.domain.Name;
import lombok.Data;

import java.time.Instant;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class NameUpdated implements CustomerEvent {
    private String customerId;
    private Name name;
    private Instant timestamp;

}
