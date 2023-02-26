package io.kineticedge.ks101.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Customer360 {

    private String customerId;
    private Name name;

    private List<Historical<Name>> names = new ArrayList<>();
    private List<Historical<Email>> emails = new ArrayList<>();
    private List<Historical<Phone>> phones = new ArrayList<>();
}
