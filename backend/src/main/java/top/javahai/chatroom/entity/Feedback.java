package top.javahai.chatroom.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class Feedback {
    private Integer id;
    private Integer userId;      // 用户ID
    private String nickname;     // 用户昵称
    private String username;     // 用户名
    private String content;      // 反馈内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;     // 创建时间

    private Integer status;      // 处理状态：0未处理，1已处理
}