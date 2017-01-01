/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.attributes;

import org.mule.extension.email.api.EmailFlags;
import org.mule.extension.email.api.exception.EmailAttributesException;

import com.sun.mail.imap.IMAPFolder;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Contains all the metadata of a received email from an IMAP mailbox, it carries information such as the subject of the email,
 * the id in the mailbox and the recipients between others but also this attributes carry flags that mark different states of
 * the email such as SEEN, ANSWERED, DELETED, etc. This flags are represented by an {@link EmailFlags} object.
 *
 * @since 4.0
 */
public class IMAPEmailAttributes extends BaseEmailAttributes {

  /**
   * The flags set in the email.
   */
  private final EmailFlags flags;

  /**
   * The unique identifier of the email in an IMAP mailbox folder.
   */
  private final long id;

  public IMAPEmailAttributes(Message msg, IMAPFolder folder) {
    super(msg);
    try {
      this.id = folder.getUID(msg);
      this.flags = new EmailFlags(msg.getFlags());
    } catch (MessagingException mse) {
      throw new EmailAttributesException(mse.getMessage(), mse);
    }
  }

  /**
   * @return an {@link EmailFlags} object containing the flags set in the email.
   */
  public EmailFlags getFlags() {
    return flags;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return id;
  }

}
