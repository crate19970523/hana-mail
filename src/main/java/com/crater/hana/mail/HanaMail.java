package com.crater.hana.mail;

import com.crater.hana.mail.exception.HanaMailException;
import com.crater.hana.mail.model.MailContent;
import com.crater.hana.mail.model.MailUserInfo;

import javax.mail.MessagingException;
import java.util.List;

public interface HanaMail {
    List<MailContent> readMail(MailUserInfo mailUserInfo, String targetMailSubjectRegex, String targetMailFolder);

    void moveMailFolder(MailUserInfo mailUserInfo, String targetMailSubjectRegex, String fromMailFolder,
                        String toMailFolder) throws HanaMailException;

    /**
     * not support annex now
     * @param mailUserInfo
     * @param mailContent
     * @throws MessagingException
     */
    void sendMail(MailUserInfo mailUserInfo, MailContent mailContent) throws MessagingException;
}
