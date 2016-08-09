/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;


import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.retriever.imap.IMAPConfiguration;
import org.mule.extension.email.internal.retriever.pop3.POP3Configuration;
import org.mule.extension.email.internal.sender.SMTPConfiguration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;

/**
 * Email connector used to list and send emails and perform operations in different mailboxes, such as delete and mark as read.
 * <p>
 * This connector supports the SMTP, SMTPS, IMAP, IMAPS, POP3 and POP3s protocols.
 *
 * @since 4.0
 */
@Configurations({SMTPConfiguration.class, POP3Configuration.class, IMAPConfiguration.class})
@Extension(name = "Email", description = "Connector to send and list email messages to and from mailboxes")
@Export(classes = EmailException.class)
public class EmailConnector {

  public static final String TLS_CONFIGURATION = "TLS Configuration";
}
