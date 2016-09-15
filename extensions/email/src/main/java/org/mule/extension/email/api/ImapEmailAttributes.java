/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.extension.email.api.exception.EmailException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Contains all the basic metadata of a received email from an IMAP mailbox, it carries information such as the subject of the email,
 * the id in the mailbox and the recipients between others but also this attributes carry flags that mark different states of
 * the email such as SEEN, ANSWERED, DELETED, etc. This flags are represented by an {@link EmailFlags} object.
 *
 * @since 4.0
 */
public class ImapEmailAttributes implements EmailAttributes {

  /**
   * The flags set in the email.
   */
  private final EmailFlags flags;

  /**
   * A basic email attributes instance delegate, to carry all the common attributes.
   */
  private final BasicEmailAttributes delegate;

  public ImapEmailAttributes(Message msg) {
    try {
      this.delegate = new BasicEmailAttributes(msg);
      this.flags = new EmailFlags(msg.getFlags());
    } catch (MessagingException mse) {
      throw new EmailException(mse.getMessage(), mse);
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
  public int getId() {
    return delegate.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getReplyToAddresses() {
    return delegate.getReplyToAddresses();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSubject() {
    return delegate.getSubject();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getToAddresses() {
    return delegate.getToAddresses();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getBccAddresses() {
    return delegate.getBccAddresses();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getCcAddresses() {
    return delegate.getCcAddresses();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getFromAddresses() {
    return delegate.getFromAddresses();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getReceivedDate() {
    return delegate.getReceivedDate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getSentDate() {
    return delegate.getSentDate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getHeaders() {
    return delegate.getHeaders();
  }

}
