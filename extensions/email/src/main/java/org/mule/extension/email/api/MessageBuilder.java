/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.MULTIPART;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.toAddressArray;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.util.EmailConnectorUtils;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * an implementation of the builder design pattern to create a new {@link Message} instance.
 *
 * @since 4.0
 */
public final class MessageBuilder {

  private static final String ERROR = "Error while creating Message";

  private final MimeMessage message;

  private Map<String, DataHandler> attachments;
  private String content = "";
  private MediaType contentType;
  private String charset;

  private MessageBuilder(Session s) throws MessagingException {
    this.message = new MimeMessage(s);
  }

  /**
   * Creates a new {@link MessageBuilder} instance for the specified {@code session}.
   *
   * @param session the {@link Session} for which the message is going to be created
   * @return a new {@link MessageBuilder} instance.
   */
  public static MessageBuilder newMessage(Session session) {
    try {
      return new MessageBuilder(session);
    } catch (MessagingException e) {
      throw new EmailException(ERROR, e);
    }
  }

  /**
   * Adds the subject to the {@link Message} that is being built.
   *
   * @param subject the subject of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withSubject(String subject) throws MessagingException {
    this.message.setSubject(subject);
    return this;
  }

  /**
   * Adds the from addresses to the {@link Message} that is being built.
   *
   * @param fromAddresses the from addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder fromAddresses(List<String> fromAddresses) throws MessagingException {
    this.message.addFrom(toAddressArray(fromAddresses));
    return this;
  }

  /**
   * Adds a single from address to the {@link Message} that is being built.
   *
   * @param from the from address of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder fromAddresses(String from) throws MessagingException {
    if (from != null) {
      this.message.setFrom(EmailConnectorUtils.toAddress(from));
    } else {
      this.message.setFrom();
    }
    return this;
  }

  /**
   * Adds the "to" (primary) addresses to the {@link Message} that is being built.
   *
   * @param toAddresses the primary addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder to(List<String> toAddresses) throws MessagingException {
    if (toAddresses != null) {
      this.message.setRecipients(TO, toAddressArray(toAddresses));
    }
    return this;
  }

  /**
   * Adds the "bcc" addresses to the {@link Message} that is being built.
   *
   * @param bccAddresses the blind carbon copy addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder bcc(List<String> bccAddresses) throws MessagingException {
    if (bccAddresses != null) {
      this.message.setRecipients(BCC, toAddressArray(bccAddresses));
    }
    return this;
  }

  /**
   * Adds the "cc" addresses to the {@link Message} that is being built.
   *
   * @param ccAddresses the carbon copy addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder cc(List<String> ccAddresses) throws MessagingException {

    if (ccAddresses != null) {
      this.message.setRecipients(CC, toAddressArray(ccAddresses));
    }
    return this;
  }

  /**
   * Adds custom headers to the {@link Message} that is being built.
   *
   * @param headers the custom headers of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withHeaders(Map<String, String> headers) throws MessagingException {
    if (headers != null) {
      for (Entry<String, String> entry : headers.entrySet()) {
        this.message.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Adds attachments represented as {@link EmailAttachment}s to the {@link Message} that is being built.
   *
   * @param attachments the attachments that are going to be added to the email.
   * @return this {@link MessageBuilder}
   */
  public MessageBuilder withAttachments(List<EmailAttachment> attachments) {
    Map<String, DataHandler> attachmentsMap = new HashMap<>();
    attachments.forEach(a -> {
      try {
        DataHandler dataHandler = toDataHandler(a.getId(), a.getContent(), a.getContentType());
        attachmentsMap.put(a.getId(), dataHandler);
      } catch (Exception e) {
        throw new EmailException(ERROR + " could not add attachments", e);
      }
    });
    this.attachments = attachmentsMap;
    return this;
  }

  /**
   * Adds attachments represented as a {@link Map} of {@link String}/{@link DataHandler} to the {@link Message} that is being
   * built.
   *
   * @param attachments the attachments that are going to be added to the email.
   * @return this {@link MessageBuilder}
   */
  public MessageBuilder withAttachments(Map<String, DataHandler> attachments) {
    this.attachments = attachments;
    return this;
  }

  /**
   * Adds the sent date to the {@link Message} that is being built.
   *
   * @param date the date in which the email was sent.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withSentDate(Date date) throws MessagingException {
    this.message.setSentDate(date);
    return this;
  }

  /**
   * Adds the text content the {@link Message} that is being built.
   *
   * @param content the text content of the email.
   * @param contentType the contentType of the {@code content} of the email. One of "text/plain" or "text/html" expected.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withContent(String content, MediaType contentType, String charset) throws MessagingException {
    this.content = content;
    this.contentType = contentType;
    this.charset = charset;
    return this;
  }

  /**
   * Adds the text content the {@link Message} that is being built. The contentType of this content will default to "text/plain".
   *
   * @param content the text content of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withContent(String content) throws MessagingException {
    this.content = content;
    this.contentType = TEXT;
    return this;
  }

  /**
   * Adds the reply to addresses of the {@link Message} that is being built.
   *
   * @param replyAddresses the reply to addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder replyTo(List<String> replyAddresses) throws MessagingException {
    this.message.setReplyTo(toAddressArray(replyAddresses));
    return this;
  }

  /**
   * Builds the {@link Message} with all the data provided.
   *
   * @return a new {@link MimeMessage} instance.
   * @throws MessagingException
   */
  public MimeMessage build() throws MessagingException {
    if (attachments != null && !attachments.isEmpty()) {
      MimeMultipart multipart = new MimeMultipart();
      MimeBodyPart bodyPart = new MimeBodyPart();
      bodyPart.setDisposition(INLINE);
      bodyPart.setContent(content, getBodyType().toRfcString());
      multipart.addBodyPart(bodyPart);

      MimeBodyPart attachmentPart;
      for (String attachment : attachments.keySet()) {
        try {
          attachmentPart = new MimeBodyPart();
          attachmentPart.setDisposition(ATTACHMENT);
          attachmentPart.setFileName(attachment);
          DataHandler attachmentDataHandler = attachments.get(attachment);
          attachmentPart.setDataHandler(attachmentDataHandler);
          multipart.addBodyPart(attachmentPart);
        } catch (Exception e) {
          throw new EmailException("Error while adding attachment: " + attachment, e);
        }
      }

      this.message.setContent(multipart, MULTIPART);
    } else {
      this.message.setDisposition(INLINE);
      this.message.setContent(content, getBodyType().toRfcString());
    }

    return message;
  }

  private MediaType getBodyType() {
    if (charset != null) {
      return DataType.builder().mediaType(contentType).charset(Charset.forName(charset)).build().getMediaType();
    }
    return contentType;
  }
}
