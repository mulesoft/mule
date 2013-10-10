/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;

import java.util.Properties;

import javax.mail.URLName;

/**
 * This class just sets some extra SMTP properties so it works with GMail.
 */
public class GmailSmtpConnector extends SmtpConnector
{

    public GmailSmtpConnector(MuleContext context)
    {
        super(context);
    }
    
    @Override
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url) 
    {
        super.extendPropertiesForSession(global, local, url);

        local.setProperty("mail.smtp.starttls.enable", "true");
        local.setProperty("mail.smtp.auth", "true");
        local.setProperty("mail.smtps.starttls.enable", "true");
        local.setProperty("mail.smtps.auth", "true");
    }
}


