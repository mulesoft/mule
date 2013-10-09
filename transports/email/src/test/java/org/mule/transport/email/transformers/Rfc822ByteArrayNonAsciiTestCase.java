/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.transformers;

import org.mule.config.i18n.LocaleMessageHandler;

import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Rfc822ByteArrayNonAsciiTestCase extends Rfc822ByteArrayTestCase 
{
    @Override
    protected MimeMessage newMimeMessage() throws MessagingException
    {
        MimeMessage message = new MimeMessage(newSession());
        String text = LocaleMessageHandler.getString("test-data", Locale.JAPAN, 
            "Rfc822ByteArrayNonAsciiTestCase.newMimeMessage", new Object[] {});
        message.setText(text, "iso-2022-jp");
        message.setSubject(text, "iso-2022-jp");
        message.setFrom(new InternetAddress("bob@example.com"));
        return message;
    }
}
