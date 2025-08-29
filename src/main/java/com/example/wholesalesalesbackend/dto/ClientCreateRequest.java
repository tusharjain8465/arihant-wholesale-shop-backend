package com.example.wholesalesalesbackend.dto;


// ClientCreateRequest.java
import lombok.Data;

@Data
public class ClientCreateRequest {
    private String name;
    private String location;
    private String contact;
}
