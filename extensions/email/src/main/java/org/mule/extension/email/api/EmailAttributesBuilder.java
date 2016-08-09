/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static javax.mail.Flags.Flag.ANSWERED;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.DRAFT;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;

import org.mule.extension.email.api.exception.EmailException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * an implementation of the builder design pattern to create a new {@link EmailAttributes} instance.
 *
 * @since 4.0
 */
public final class EmailAttributesBuilder {

  private int id;
  private String subject;
  private List<String> from = new ArrayList<>();
  private List<String> to = new ArrayList<>();
  private List<String> bcc = new ArrayList<>();
  private List<String> cc = new ArrayList<>();
  private Map<String, String> headers = new HashMap<>();
  private LocalDateTime sentDate;
  private LocalDateTime receivedDate;
  private List<String> replyTo = new ArrayList<>();
  private boolean answered;
  private boolean deleted;
  private boolean draft;
  private boolean recent;
  private boolean seen;

  /**
   * Hide constructor.
   */
  private EmailAttributesBuilder() {}

  /**
   * builds the new {@link EmailAttributes} instance from a given {@link Message} extracting all its attributes including the
   * attachments.
   *
   * @param msg the {@link Message} to extract attributes from.
   * @return a new {@link EmailAttributes} instance.
   */
  public static EmailAttributes fromMessage(Message msg) {
    try {
      Flags flags = msg.getFlags();

      EmailAttributesBuilder builder =
          EmailAttributesBuilder.newAttributes().withId(msg.getMessageNumber()).withSubject(msg.getSubject())
              .fromAddresses(msg.getFrom()).toAddresses(msg.getRecipients(TO)).ccAddresses(msg.getRecipients(CC))
              .bccAddresses(msg.getRecipients(BCC)).seen(flags.contains(SEEN)).replyToAddress(msg.getReplyTo())
              .recent(flags.contains(RECENT)).sentDate(msg.getSentDate()).receivedDate(msg.getReceivedDate())
              .draft(flags.contains(DRAFT)).answered(flags.contains(ANSWERED)).deleted(flags.contains(DELETED));

      return builder.build();
    } catch (MessagingException mse) {
      throw new EmailException(mse.getMessage(), mse);
    }
  }

  /**
   * @return an instance of this {@link EmailAttributesBuilder}.
   */
  public static EmailAttributesBuilder newAttributes() {
    return new EmailAttributesBuilder();
  }

  /**
   * set the email number.
   *
   * @param id the number of the email
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder withId(int id) {
    this.id = id;
    return this;
  }

  /**
   * set the subject of the email.
   *
   * @param subject the email subject to be set in the attributes
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder withSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * set the email from addresses
   *
   * @param fromAddresses the addresses to be set in the attributes.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder fromAddresses(Address[] fromAddresses) {
    addArrayAddresses(fromAddresses, from);
    return this;
  }

  /**
   * set the "To" (primary) recipients of the email.
   *
   * @param toAddresses the "to" addresses to be set.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder toAddresses(Address[] toAddresses) {
    addArrayAddresses(toAddresses, to);
    return this;
  }

  /**
   * sets the "Bcc" (blind carbon copy) recipients of the email.
   *
   * @param bccAddresses the "bcc" addresses to be set.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder bccAddresses(Address[] bccAddresses) {
    addArrayAddresses(bccAddresses, bcc);
    return this;
  }

  /**
   * set the "Cc" (carbon copy) recipients of the email.
   *
   * @param ccAddresses the "cc" addresses to be set.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder ccAddresses(Address[] ccAddresses) {
    addArrayAddresses(ccAddresses, cc);
    return this;
  }

  /**
   * set the additional headers of the email.
   *
   * @param headers the headers to be set.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * set the date when the message was sent.
   *
   * @param sentDate the date when the message was sent.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder sentDate(Date sentDate) {
    this.sentDate = asDateTime(sentDate);
    return this;
  }

  /**
   * set the date when the message was received.
   *
   * @param receivedDate
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder receivedDate(Date receivedDate) {
    this.receivedDate = asDateTime(receivedDate);
    return this;
  }

  /**
   * set the "ReplyTo" addresses of the email.
   *
   * @param replyToAddresses the "replyTo" addresses to be set.
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder replyToAddress(Address[] replyToAddresses) {
    addArrayAddresses(replyToAddresses, replyTo);
    return this;
  }

  /**
   * adds the ANSWERED flag to the email attributes.
   *
   * @param answered if the email is marked as ANSWERED or not
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder answered(boolean answered) {
    this.answered = answered;
    return this;
  }

  /**
   * adds the DELETED flag to the email attributes.
   *
   * @param deleted if the email is marked as DELETED or not
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder deleted(boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  /**
   * adds the DRAFT flag to the email attributes.
   *
   * @param draft if the email is marked as DRAFT or not
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder draft(boolean draft) {
    this.draft = draft;
    return this;
  }

  /**
   * adds the RECENT flag to the email attributes.
   *
   * @param recent if the email is marked as RECENT or not
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder recent(boolean recent) {
    this.recent = recent;
    return this;
  }

  /**
   * adds the SEEN flag to the email attributes.
   *
   * @param seen if the email is marked as SEEN or not
   * @return this {@link EmailAttributesBuilder}
   */
  public EmailAttributesBuilder seen(boolean seen) {
    this.seen = seen;
    return this;
  }

  /**
   * builds the new {@link EmailAttributes} instance.
   *
   * @return a new {@link EmailAttributes} instance.
   */
  public EmailAttributes build() {
    return new EmailAttributes(id, subject, from, to, bcc, cc, replyTo, headers, receivedDate, sentDate,
                               new EmailFlags(answered, deleted, draft, recent, seen));
  }

  private void addArrayAddresses(Address[] toAddresses, List<String> addresses) {
    if (toAddresses != null) {
      Arrays.stream(toAddresses).map(Object::toString).forEach(addresses::add);
    }
  }

  private LocalDateTime asDateTime(Date date) {
    if (date != null) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}

