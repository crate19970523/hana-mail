package com.crater.hana.mail.model;

import com.crater.hana.mail.controller.ImapMailController;

import java.io.File;
import java.util.Date;
import java.util.List;

public class MailContent {
    private String subject;
    private String content;
    private List<MailAnnex> annex;
    private List<String> MailFrom;
    private Date mailReceivedDate;
    private List<String> mailTo;

    public String getSubject() {
        return subject;
    }

    public MailContent setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getContent() {
        return content;
    }

    public MailContent setContent(String content) {
        this.content = content;
        return this;
    }

    public List<MailAnnex> getAnnex() {
        return annex;
    }

    public MailContent setAnnex(List<MailAnnex> annex) {
        this.annex = annex;
        return this;
    }

    public List<String> getMailFrom() {
        return MailFrom;
    }

    public MailContent setMailFrom(List<String> mailFrom) {
        MailFrom = mailFrom;
        return this;
    }

    public Date getMailReceivedDate() {
        return mailReceivedDate;
    }

    public MailContent setMailReceivedDate(Date mailReceivedDate) {
        this.mailReceivedDate = mailReceivedDate;
        return this;
    }

    public List<String> getMailTo() {
        return mailTo;
    }

    public MailContent setMailTo(List<String> mailTo) {
        this.mailTo = mailTo;
        return this;
    }
}
