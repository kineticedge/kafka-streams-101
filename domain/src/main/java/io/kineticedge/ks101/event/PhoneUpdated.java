package io.kineticedge.ks101.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kineticedge.ks101.domain.Phone;
import lombok.Data;

import java.time.Instant;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class PhoneUpdated implements CustomerEvent {
    private String customerId;
    private Phone phone;
    private Instant timestamp;

}
