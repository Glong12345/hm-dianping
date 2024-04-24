package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    /**
     * 发送邮箱
     */
    private String toEmail;

    /**
     * 主题
     */
    private String subject;

    /**
     * 内容
     */
    private String content;

}
