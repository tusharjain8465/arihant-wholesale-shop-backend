package com.example.wholesalesalesbackend.dto;

public class Msg91OtpRequest {
    private String mobile;
    private String otp;
    private String sender;
    private String template_id;

    public Msg91OtpRequest(String mobile, String otp, String sender, String template_id) {
        this.mobile = mobile;
        this.otp = otp;
        this.sender = sender;
        this.template_id = template_id;
    }

    // Getters and Setters
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getTemplate_id() { return template_id; }
    public void setTemplate_id(String template_id) { this.template_id = template_id; }
}
