/*
 * $$Id$$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.bookstore.utils;

import java.util.Properties;

import javax.mail.URLName;

/**
 * This class just sets some extra SMTP properties so it works with GMail.
 */
public class SmtpConnector extends org.mule.transport.email.SmtpsConnector
{

    @Override
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url) {
        super.extendPropertiesForSession(global, local, url);

        local.setProperty("mail.smtp.starttls.enable", "true");
        local.setProperty("mail.smtp.auth", "true");
        local.setProperty("mail.smtps.starttls.enable", "true");
        local.setProperty("mail.smtps.auth", "true");
    }
}
