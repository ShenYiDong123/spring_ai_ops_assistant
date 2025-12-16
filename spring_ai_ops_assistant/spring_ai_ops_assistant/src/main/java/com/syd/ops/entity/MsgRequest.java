package com.syd.ops.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MsgRequest {

    private String msg;       // 消息内容（包含特殊字符）

    private String modelType; // 模型类型
}
