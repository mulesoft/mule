/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static javax.mail.Part.ATTACHMENT;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.TEXT;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * Given a {@link Message} introspects it's content to obtain the body an the attachments if any.
 *
 * @since 4.0
 */
public class EmailContentProcessor {

  private static final String ERROR_PROCESSING_MESSAGE = "Error while processing message content.";

  private final List<Message> attachmentParts = new LinkedList<>();
  private final String body;

  /**
   * Creates an instance and process the message content.
   * <p>
   * Hided constructor, can only get a new instance out of this class using the factory method
   * {@link EmailContentProcessor#getInstance(javax.mail.Message)}.
   *
   * @param message the {@link javax.mail.Message} to be processed.
   */
  private EmailContentProcessor(javax.mail.Message message) {
    StringJoiner bodyCollector = new StringJoiner("\n");
    processPart(message, bodyCollector);
    body = bodyCollector.toString();
  }

  /**
   * Factory method to get a new instance of {@link EmailContentProcessor} and process a {@link Message}.
   *
   * @param message the {@link Message} to be processed.
   * @return a new {@link EmailContentProcessor} instance.
   */
  public static EmailContentProcessor getInstance(javax.mail.Message message) {
    return new EmailContentProcessor(message);
  }

  /**
   * @return the text body of the message.
   */
  public String getBody() {
    return body.trim();
  }

  /**
   * @return a {@link List} with the attachments of an email bounded into {@link Message}s.
   */
  public List<Message> getAttachments() {
    return ImmutableList.copyOf(attachmentParts);
  }

  /**
   * Processes a single {@link Part} and adds it to the body of the message or as a new attachment depending on it's disposition
   * type.
   *
   * @param part the part to be processed
   */
  private void processPart(Part part, StringJoiner bodyCollector) {
    try {
      Object content = part.getContent();
      if (isMultipart(content)) {
        Multipart mp = (Multipart) part.getContent();
        for (int i = 0; i < mp.getCount(); i++) {
          processPart(mp.getBodyPart(i), bodyCollector);
        }
      }

      if (isAttachment(part)) {
        processAttachment(part);
      } else {
        processBodyPart(part, bodyCollector, content);
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException(ERROR_PROCESSING_MESSAGE, e);
    }
  }

  /**
   * Processes a body part, adding it to the body that is being built.
   *
   * @param part the attachment part to be processed
   */
  private void processBodyPart(Part part, StringJoiner bodyCollector, Object content) throws MessagingException {
    if (isText(content)) {
      bodyCollector.add((String) content);
    }

    if (content instanceof InputStream && isInline(part) && part.isMimeType(TEXT)) {
      String inline = IOUtils.toString((InputStream) content);
      bodyCollector.add(inline);
    }
  }

  /**
   * Processes an attachment part, adding it to the attachments list.
   *
   * @param part the attachment part to be processed
   */
  private void processAttachment(Part part) throws MessagingException, IOException {
    Map<String, LinkedList<String>> headers = new HashMap<>();
    final Enumeration allHeaders = part.getAllHeaders();
    while (allHeaders.hasMoreElements()) {
      Header h = (Header) allHeaders.nextElement();
      LinkedList<String> headerVal = new LinkedList<>();
      headerVal.add(h.getValue());
      headers.put(h.getName(), headerVal);
    }

    attachmentParts.add(Message.builder()
        .payload(part.getInputStream())
        .mediaType(MediaType.parse(part.getContentType()))
        .attributes(new PartAttributes(part.getFileName(), part.getFileName(), part.getSize(), headers))
        .build());
  }

  /**
   * Evaluates whether the disposition of the {@link Part} is INLINE or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as inline, false otherwise
   */
  private boolean isInline(Part part) throws MessagingException {
    return part.getDisposition().equalsIgnoreCase(Part.INLINE);
  }


  /**
   * Evaluates whether a {@link Part} is an attachment or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as an attachment, false otherwise
   */
  private boolean isAttachment(Part part) throws MessagingException {
    return part.getFileName() != null && (part.getDisposition() == null || part.getDisposition().equals(ATTACHMENT));
  }

  /**
   * Evaluates whether a content is multipart or not.
   *
   * @param content the content to be evaluated.
   * @return true if is multipart, false otherwise
   */
  private boolean isMultipart(Object content) {
    return content instanceof Multipart;
  }

  /**
   * Evaluates whether a content is text or not.
   *
   * @param content the content to be evaluated.
   * @return true if is text, false otherwise
   */
  private boolean isText(Object content) {
    return content instanceof String;
  }
}
