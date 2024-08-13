package com.atguigu.tingshu.album.pojo;

import lombok.Data;

/**
 * @author xk
 * @since 2024-08-12 18:31
 */
@Data
public class MaxWellObj {
    private String database;
    private String table;
    private String type;
    private Long ts;
    private Long xid;
    private Boolean commit;
    private String data;
    private String old;
}
