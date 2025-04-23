package com.voltpay.voltpay_writer.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReadEvent {

    // Ignoring because we don't need to serializer/deserialize the id of the message in Read Service.
    @JsonIgnore
    private String messageId;

    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private BigDecimal amount;

    private Integer status;

    private String currency;

    private Long custId;

    private String type;

    private String comment;

    private Integer version;
}
