package io.kineticedge.ks101.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kineticedge.ks101.domain.Email;
import io.kineticedge.ks101.domain.Name;
import io.kineticedge.ks101.domain.Phone;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class CustomerUpdated implements CustomerEvent {
    private String customerId;
    private Name name;
    private Email email;
    private Phone phone;
    private Instant timestamp;
}
