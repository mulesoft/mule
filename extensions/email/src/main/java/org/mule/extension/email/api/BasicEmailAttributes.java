/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.core.message.BaseAttributes;
import org.mule.runtime.core.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Contains all the metadata of a received email, it carries information such as the subject of the email,
 * the id in the mailbox and the recipients between others.
 * <p>
 * This class aims to be returned as attributes in a {@link org.mule.runtime.api.message.Message} for every retriever operation.
 *
 * @since 4.0
 */
public class BasicEmailAttributes extends BaseAttributes implements EmailAttributes {

  /**
   * The id is the relative position of the email in its Folder. Note that the id for a particular email can change during a
   * session if other emails in the Folder are isDeleted and expunged.
   * <p>
   * Valid message ids start at 1. Emails that do not belong to any folder (like newly composed or derived messages) have 0 as
   * their message id.
   */
  private final int id;

  /**
   * The address(es) of the person(s) which sent the email.
   * <p>
   * This will usually be the sender of the email, but some emails may direct replies to a different address
   */
  private final List<String> fromAddresses;

  /**
   * The recipient addresses of "To" (primary) type.
   */
  private final List<String> toAddresses;

  /**
   * The recipient addresses of "Cc" (carbon copy) type
   */
  private final List<String> ccAddresses;

  /**
   * The recipient addresses of "Bcc" (blind carbon copy) type
   */
  private final List<String> bccAddresses;

  /**
   * The email addresses to which this email should reply.
   */
  private final List<String> replyToAddresses;

  /**
   * The headers that this email carry.
   */
  private final Map<String, String> headers;

  /**
   * The subject of the email.
   */
  private final String subject;

  /**
   * The time where the email was received.
   * <p>
   * Different {@link Folder} implementations may assign this value or not.
   * <p>
   * If this is a sent email this will be null.
   */
  private final LocalDateTime receivedDate;

  /**
   * The time where the email was sent.
   * <p>
   * Different {@link Folder} implementations may assign this value or not.
   */
  private final LocalDateTime sentDate;

  public BasicEmailAttributes(Message msg) {
    try {
      Map<String, String> headers = new HashMap<>();
      list(msg.getAllHeaders()).forEach(h -> headers.put(((Header) h).getName(), ((Header) h).getValue()));

      this.id = msg.getMessageNumber();
      this.subject = msg.getSubject();
      this.headers = ImmutableMap.copyOf(headers);
      this.toAddresses = addressesAsList(msg.getRecipients(TO));
      this.ccAddresses = addressesAsList(msg.getRecipients(CC));
      this.bccAddresses = addressesAsList(msg.getRecipients(BCC));
      this.replyToAddresses = addressesAsList(msg.getReplyTo());
      this.sentDate = asDateTime(msg.getSentDate());
      this.receivedDate = asDateTime(msg.getReceivedDate());
      this.fromAddresses = addressesAsList(msg.getFrom());

    } catch (MessagingException mse) {
      throw new EmailException(mse.getMessage(), mse);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getReplyToAddresses() {
    return replyToAddresses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSubject() {
    return subject;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getToAddresses() {
    return toAddresses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getBccAddresses() {
    return bccAddresses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getCcAddresses() {
    return ccAddresses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getFromAddresses() {
    return fromAddresses;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getReceivedDate() {
    return receivedDate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalDateTime getSentDate() {
    return sentDate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getHeaders() {
    return headers != null ? ImmutableMap.copyOf(headers) : ImmutableMap.of();
  }

  private List<String> addressesAsList(Address[] toAddresses) {
    return toAddresses != null ? stream(toAddresses).map(Object::toString).collect(new ImmutableListCollector<>()) : emptyList();
  }

  private LocalDateTime asDateTime(Date date) {
    if (date != null) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
