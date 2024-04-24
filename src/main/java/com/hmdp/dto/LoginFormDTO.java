package com.hmdp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginFormDTO {
    @JsonProperty("phone")
    private String email;
    private String code;
    private String password;
}
