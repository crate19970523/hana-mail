package com.crater.hana.mail.model;

public class MailUserInfo {
    private String acount;
    private String host;
    private String port;
    private String password;

    public String getAcount() {
        return acount;
    }

    public MailUserInfo setAcount(String acount) {
        this.acount = acount;
        return this;
    }

    public String getHost() {
        return host;
    }

    public MailUserInfo setHost(String host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public MailUserInfo setPort(String port) {
        this.port = port;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public MailUserInfo setPassword(String password) {
        this.password = password;
        return this;
    }
}
