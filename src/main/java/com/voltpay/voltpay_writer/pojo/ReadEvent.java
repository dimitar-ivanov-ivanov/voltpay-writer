package com.voltpay.voltpay_writer.pojo;

import com.voltpay.voltpay_writer.entities.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReadEvent {

    private PrimaryKey id;

    private BigDecimal amount;

    private Integer status;

    private String currency;

    private Long custId;

    private String type;

    private String comment;

    private Integer version;
}
