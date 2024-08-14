package com.atguigu.tingshu.vo.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "声音购买模式列表")
public class TrackOrderVo {

    @Schema(description = "模式名称：本集、后10集、后20集....")
    private String name;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "购买集数")
    private Integer trackCount;
}