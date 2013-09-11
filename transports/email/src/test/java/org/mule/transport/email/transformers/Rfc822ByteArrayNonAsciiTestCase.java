/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
