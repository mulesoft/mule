/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Map;

/**
 * Enables the creation of an outgoing email. Users must use this builder
 * to create a email instance from xml to be sent over SMTP.
 *
 * @since 4.0
 */
public class EmailBuilder {

  /**
   * The "From" sender address. The person that is going to send the messages,
   * if not set, it defaults to the from address specified in the config.
   */
  @Optional
  @Parameter
  private String fromAddress;

  /**
   * The recipient addresses of "To" (primary) type.
   */
  @Parameter
  private List<String> toAddresses;

  /**
   * The recipient addresses of "Cc" (carbon copy) type
   */
  @Optional
  @Parameter
  @NullSafe
  private List<String> ccAddresses;

  /**
   * The recipient addresses of "Bcc" (blind carbon copy) type
   */
  @Optional
  @Parameter
  @NullSafe
  private List<String> bccAddresses;

  /**
   * The email addresses to which this email should reply.
   */
  @Optional
  @Parameter
  @NullSafe
  private List<String> replyToAddresses;

  /**
   * The subject of the email.
   */
  @Optional(defaultValue = "[No Subject]")
  @Parameter
  private String subject;

  /**
   * The customHeaders that this email carry.
   */
  @Optional
  @Parameter
  @Content
  @NullSafe
  private Map<String, String> customHeaders;

  public EmailAttachment getAttachment() {
    return attachment;
  }

  public void setAttachment(EmailAttachment attachment) {
    this.attachment = attachment;
  }

  @Optional
  @Parameter
  @Content
  private EmailAttachment attachment;

  @Optional
  @Parameter
  @Content
  @NullSafe
  private List<EmailAttachment> attachments;

  private EmailBody body;

  /**
   * Creates a new instance.
   */
  public EmailBuilder() {}

  /**
   * @return the configured subject for the built outgoing email.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return a {@link List} with the addresses that this mail should be replied to configured in the built outgoing email.
   */
  public List<String> getReplyToAddresses() {
    return ensureNotNullList(replyToAddresses);
  }

  /**
   * @return a {@link List} with the primary recipients configured in the built outgoing email.
   */
  public List<String> getToAddresses() {
    return ensureNotNullList(toAddresses);
  }

  /**
   * @return a {@link List} with the cc recipients configured in the built outgoing email.
   */
  public List<String> getCcAddresses() {
    return ensureNotNullList(ccAddresses);
  }

  /**
   * @return a {@link List} with the bcc recipients configured in the built outgoing email.
   */
  public List<String> getBccAddresses() {
    return ensureNotNullList(bccAddresses);
  }

  /**
   * @return a {@link Map} with all the additional customHeaders configured for the
   */
  public Map<String, String> getCustomHeaders() {
    return customHeaders != null ? customHeaders : emptyMap();
  }

  /**
   * @return the body of built outgoing email.
   */
  public EmailBody getBody() {
    return body == null ? new EmailBody() : body;
  }

  public EmailBody setBody(EmailBody emailBody) {
    return body = emailBody;
  }

  /**
   * @return the from address, the address of the sender of the email.
   */
  public String getFromAddress() {
    return fromAddress;
  }

  /**
   * @return a {@link List} of attachments configured in the built outgoing email.
   */
  public List<EmailAttachment> getAttachments() {
    return ensureNotNullList(attachments);
  }

  /**
   * @return an emptyList if the list is {@code null}, the {@link List} instance otherwise.
   */
  private <T> List<T> ensureNotNullList(List<T> list) {
    return list != null ? list : emptyList();
  }

  public void setAttachments(List<EmailAttachment> attachments) {
    this.attachments = attachments;
  }
}
