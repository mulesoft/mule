/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.message.BaseAttributes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * Contains all the metadata of an email, it carries information such as the subject of the email, the id in the mailbox and the
 * recipients between others.
 * <p>
 * This class aims to be returned as attributes in a {@link MuleMessage} for every retriever operation.
 * <p>
 * The attachments of the email are also carried in an {@link EmailAttributes} instance and separated from the original multipart
 * {@link Message}.
 *
 * @since 4.0
 */
public class EmailAttributes extends BaseAttributes {

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

  /**
   * The flags setted in the email.
   */
  private final EmailFlags flags;

  /**
   * Creates a new instance.
   *
   * @param id the id of the email.
   * @param subject the subject of the email
   * @param fromAddresses the addresses that are sending the email.
   * @param toAddresses the primary addresses to deliver the email.
   * @param bccAddresses the blind carbon copy addresses to deliver the email.
   * @param ccAddresses the carbon copy addresses to deliver the email.
   * @param replyToAddresses the addresses to reply to this message.
   * @param headers the header of the email.
   * @param receivedDate the received date of the email.
   * @param flags the {@link EmailFlags} setted on the email.
   */
  public EmailAttributes(int id, String subject, List<String> fromAddresses, List<String> toAddresses, List<String> bccAddresses,
                         List<String> ccAddresses, List<String> replyToAddresses, Map<String, String> headers,
                         LocalDateTime receivedDate, LocalDateTime sentDate, EmailFlags flags) {
    this.id = id;
    this.subject = subject;
    this.sentDate = sentDate;
    this.receivedDate = receivedDate;
    this.toAddresses = ImmutableList.copyOf(toAddresses);
    this.ccAddresses = ImmutableList.copyOf(ccAddresses);
    this.bccAddresses = ImmutableList.copyOf(bccAddresses);
    this.fromAddresses = ImmutableList.copyOf(fromAddresses);
    this.replyToAddresses = ImmutableList.copyOf(replyToAddresses);
    this.headers = ImmutableMap.copyOf(headers);
    this.flags = flags;
  }

  /**
   * Get the Message id of the email.
   *
   * @return the message id
   */
  public int getId() {
    return id;
  }

  /**
   * Get the addresses to which replies should be directed. This will usually be the sender of the email, but some emails may
   * direct replies to a different address
   *
   * @return all the recipient addresses of replyTo type.
   */
  public List<String> getReplyToAddresses() {
    return replyToAddresses;
  }

  /**
   * @return the subject of the email.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return all the recipient addresses of "To" (primary) type.
   */
  public List<String> getToAddresses() {
    return toAddresses;
  }

  /**
   * @return all the recipient addresses of "Bcc" (blind carbon copy) type.
   */
  public List<String> getBccAddresses() {
    return bccAddresses;
  }

  /**
   * @return all the recipient addresses of "Cc" (carbon copy) type.
   */
  public List<String> getCcAddresses() {
    return ccAddresses;
  }

  /**
   * Get the identity of the person(s) who wished this message to be sent.
   *
   * @return all the from addresses.
   */
  public List<String> getFromAddresses() {
    return fromAddresses;
  }

  /**
   * Get the date this message was received.
   *
   * @return the date this message was received.
   */
  public LocalDateTime getReceivedDate() {
    return receivedDate;
  }

  /**
   * Get the date this message was sent.
   *
   * @return the date this message was sent.
   */
  public LocalDateTime getSentDate() {
    return sentDate;
  }

  /**
   * @return all the headers of this email message.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * @return an {@link EmailFlags} object containing the flags setted in the email.
   */
  public EmailFlags getFlags() {
    return flags;
  }
}
