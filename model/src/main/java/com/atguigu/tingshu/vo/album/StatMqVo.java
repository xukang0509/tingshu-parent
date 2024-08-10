package com.atguigu.tingshu.vo.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "StatMqVo")
public class StatMqVo {

    @Schema(description = "业务编号：去重使用")
    private String businessNo;

    @Schema(description = "专辑id")
    private Long albumId;

    @Schema(description = "声音id")
    private Long trackId;

    @Schema(description = "声音统计类型")
    private String trackStatType;

    @Schema(description = "专辑统计类型")
    private String albumStatType;

    @Schema(description = "更新数目")
    private Integer count;

}