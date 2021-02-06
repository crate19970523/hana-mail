package com.crater.hana.mail.model;

import java.io.File;

public class MailAnnex {
    private File annexFile;
    private String annexFileName;

    public File getAnnexFile() {
        return annexFile;
    }

    public MailAnnex setAnnexFile(File annexFile) {
        this.annexFile = annexFile;
        return this;
    }

    public String getAnnexFileName() {
        return annexFileName;
    }

    public MailAnnex setAnnexFileName(String annexFileName) {
        this.annexFileName = annexFileName;
        return this;
    }
}
