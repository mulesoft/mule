/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.attributes;

import static java.lang.Long.parseLong;
import org.mule.extension.email.api.exception.EmailAttributesException;

import com.sun.mail.pop3.POP3Folder;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Contains all the metadata of a received email from a POP3 mailbox, it carries information such as the subject of the email,
 * the unique id in the mailbox and the recipients between others
 *
 * @since 4.0
 */
public class POP3EmailAttributes extends BaseEmailAttributes {

  /**
   * The unique identifier of the email in an IMAP mailbox folder.
   */
  private final long id;

  /**
   * Creates a new instance from a {@link Message}
   *
   * @param msg an email message to take the attributes from.
   */
  public POP3EmailAttributes(Message msg, POP3Folder folder) {
    super(msg);
    try {
      this.id = parseLong(folder.getUID(msg));
    } catch (MessagingException e) {
      throw new EmailAttributesException("Could not initialize POP3 attributes", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return id;
  }
}
