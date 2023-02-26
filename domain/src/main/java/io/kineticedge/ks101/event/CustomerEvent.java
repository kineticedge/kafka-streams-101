package io.kineticedge.ks101.event;

import java.time.Instant;

public interface CustomerEvent {

    String getCustomerId();

    Instant getTimestamp();
    void setTimestamp(Instant timestamp);
}
