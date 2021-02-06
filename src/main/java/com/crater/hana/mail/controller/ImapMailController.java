package com.crater.hana.mail.controller;

import com.crater.hana.mail.HanaMail;
import com.crater.hana.mail.exception.HanaMailException;
import com.crater.hana.mail.model.MailAnnex;
import com.crater.hana.mail.model.MailContent;
import com.crater.hana.mail.model.MailUserInfo;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ImapMailController implements HanaMail {

    @Override
    public void moveMailFolder(MailUserInfo mailUserInfo, String targetMailSubjectRegex, String fromMailFolder,
                               String toMailFolder) throws HanaMailException {
        try {
            var properties = generateImapProperties(mailUserInfo);
            var store = javaMailStoreConnect(properties, mailUserInfo);
            var fromImapFolder = takeIMAPFolder(store, fromMailFolder);
            var toImapFolder = takeIMAPFolder(store, toMailFolder);
            Message[] messages = takeMessageFromFolder(fromImapFolder);
            fromImapFolder.copyMessages(messages, toImapFolder);
            Arrays.asList(messages).parallelStream()
                    .filter(m -> checkMailSubjectIsLegitimateForTargetMailSubjectRegex(m, targetMailSubjectRegex))
                    .collect(Collectors.toList()).forEach(m -> {
                try {
                    m.setFlag(Flags.Flag.DELETED, true);
                } catch (MessagingException e) {
                    e.printStackTrace();  //TODO: need add HanaMailException
                }
            });

            fromImapFolder.close(true);
            toImapFolder.close(true);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new HanaMailException("move mail folder fail"); //TODO: The reason must be analyzed to make the error better understand
        }
    }

    @Override
    public List<MailContent> readMail(MailUserInfo mailUserInfo, String targetMailSubjectRegex, String targetMailFolder) {
        targetMailFolder = targetMailFolder == null ? "InBox" : targetMailFolder;
        List<MailContent> mailContents = new ArrayList<>();
        IMAPFolder imapFolder;
        try {
            var properties = generateImapProperties(mailUserInfo);
            var store = javaMailStoreConnect(properties, mailUserInfo);
            imapFolder = takeIMAPFolder(store, targetMailFolder);
            var messages = takeMessageFromFolder(imapFolder);
            mailContents = Arrays.asList(messages).parallelStream()
                    .filter(m -> checkMailSubjectIsLegitimateForTargetMailSubjectRegex(m, targetMailSubjectRegex))
                    .map(m -> mailMessageConverterToMailContext(m)).collect(Collectors.toList());
            imapFolder.close(true);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return mailContents;
    }

    private Properties generateImapProperties(MailUserInfo mailUserInfo) {
        var props = System.getProperties();
        props.setProperty("mail.imap.host", mailUserInfo.getHost());
        props.setProperty("mail.imap.port", mailUserInfo.getPort());
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.debug", "true");
        return props;
    }

    private Store javaMailStoreConnect(Properties properties, MailUserInfo mailUserInfo) throws MessagingException {
        Session session = Session.getInstance(properties);
        Store imapMailStore = session.getStore("imaps");
        imapMailStore.connect(mailUserInfo.getHost(), mailUserInfo.getAcount(), mailUserInfo.getPassword());
        return imapMailStore;
    }

    private IMAPFolder takeIMAPFolder(Store imapMailStore, String mailFolderName) throws MessagingException {
        IMAPFolder mailFolder = (IMAPFolder) imapMailStore.getFolder(mailFolderName);
        mailFolder.open(Folder.READ_WRITE);
        return mailFolder;
    }

    private Message[] takeMessageFromFolder(IMAPFolder imapFolder) throws MessagingException {
        return imapFolder.getMessages();
    }

    private boolean checkMailSubjectIsLegitimateForTargetMailSubjectRegex(Message message, String targetMailSubjectRegex) {
        boolean result = false;
        try {
            result = message.getSubject().matches(targetMailSubjectRegex);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private MailContent mailMessageConverterToMailContext(Message message) {
        MailContent result = null;
        var subject = "";
        var textContent = "";
        List<String> mailFrom = new ArrayList<>();
        Date mailReceivedDate = null;
        try {
            mailFrom = Arrays.stream(message.getFrom()).map(rfc822 -> rfc822.toString()).collect(Collectors.toList());
            mailReceivedDate = message.getReceivedDate();
            subject = message.getSubject();
            Object messageContent = message.getContent();
            if (message.isMimeType("text/plain")) textContent = processMailContentIsTextPlain(messageContent);
            else if (message.isMimeType("multipart/alternative"))
                textContent = processMailContentIsMultipartAlternative(messageContent);
            else result = processMailContentIsMultipartMIXED(messageContent);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = result == null ? new MailContent().setContent(textContent) : result;
        result.setSubject(subject).setMailFrom(mailFrom).setMailReceivedDate(mailReceivedDate);
        return result;
    }

    private MailContent processMailContentIsMultipartMIXED(Object content) throws IOException, MessagingException {
        Multipart multipart = (Multipart) content;
        int numParts = multipart.getCount();
        String contentString = "";
        List<MailAnnex> annexes = new ArrayList<>();
        for (int i = 0; i < numParts; i++) {
            File annexFile;
            Object contentInMultipart = multipart.getBodyPart(i).getContent();
            if (multipart.getBodyPart(i).isMimeType("multipart/alternative"))
                contentString = contentString + processMailContentIsMultipartAlternative(contentInMultipart);
            else if (multipart.getBodyPart(i).isMimeType("text/plain"))
                contentString = contentString + processMailContentIsTextPlain(contentInMultipart);
            else {
                MailAnnex mailAnnex = downloadAttachment(multipart.getBodyPart(i));
                annexes.add(mailAnnex);
            }
        }
        return new MailContent().setContent(contentString).setAnnex(annexes);
    }

    private String processMailContentIsMultipartAlternative(Object content) throws IOException, MessagingException {
        Multipart multipart = (Multipart) content;
        int numParts = multipart.getCount();
        StringBuilder contentString = new StringBuilder();
        for (int i = 0; i < numParts && multipart.getBodyPart(i).isMimeType("text/plain"); ++i) {
            contentString.append(multipart.getBodyPart(i).getContent());
        }
        return contentString.toString();
    }

    private String processMailContentIsTextPlain(Object content) throws IOException, MessagingException {
        return (String) content;
    }

    private MailAnnex downloadAttachment(BodyPart bodyPart) throws MessagingException, IOException {
        InputStream annexInputStream = bodyPart.getInputStream();
        var annexFile = inputStreamToFile(annexInputStream);
        return new MailAnnex().setAnnexFile(annexFile).setAnnexFileName(bodyPart.getFileName());
    }

    private File inputStreamToFile(InputStream inputStream) throws IOException {
        File file = File.createTempFile("BatchMaiil", ".temp");
        var fileOutputStream = new FileOutputStream(file);
        int count;
        while ((count = inputStream.read()) != -1) {
            fileOutputStream.write(count);
        }
        inputStream.close();
        fileOutputStream.close();
        return file;
    }

    @Override
    public void sendMail(MailUserInfo mailUserInfo, MailContent mailContent) {
        var username = mailUserInfo.getAcount();
        var password = mailUserInfo.getPassword();
        var from = username;
        var toBuild = new StringBuilder();
        mailContent.getMailTo().forEach(s -> toBuild.append(s).append(","));

        var props = generateSmtpProperties(mailUserInfo);
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toBuild.toString()));
            message.setSubject(mailContent.getSubject());
            message.setText(mailContent.getContent());
            Transport.send(message);
        } catch (AddressException e) {
            e.printStackTrace();  //TODO: need add HanaMailException
        } catch (MessagingException e) {
            e.printStackTrace();  //TODO: need add HanaMailException
        }

    }

    private Properties generateSmtpProperties(MailUserInfo mailUserInfo) {
        var props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", mailUserInfo.getHost());
        props.put("mail.smtp.port", mailUserInfo.getPort());
        return props;
    }
}
