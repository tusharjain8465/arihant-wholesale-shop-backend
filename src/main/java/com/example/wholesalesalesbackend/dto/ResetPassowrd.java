package com.example.wholesalesalesbackend.dto;

import lombok.Data;

@Data
public class ResetPassowrd {

    private String userName;

    private String mail;

    private Long mobileNumber;

    private String oldPassword;

    private String newPassword;

}
