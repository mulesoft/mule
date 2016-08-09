/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static java.util.Collections.singletonList;
import static javax.mail.Part.ATTACHMENT;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.TEXT;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

public class EmailContentProcessor {

  private static final String ERROR_PROCESSING_MESSAGE = "Error while processing message content.";

  private final List<MuleMessage> attachmentParts = new LinkedList<>();
  private final StringJoiner body = new StringJoiner("\n");

  /**
   * Creates an instance and process the message content.
   * <p>
   * Hided constructor, can only get a new instance out of this class using the factory method
   * {@link EmailContentProcessor#process(Message)}.
   *
   * @param message the {@link Message} to be processed.
   */
  private EmailContentProcessor(Message message) {
    processPart(message);
  }

  /**
   * Factory method to get a new instance of {@link EmailContentProcessor} and process a {@link Message}.
   *
   * @param message the {@link Message} to be processed.
   * @return a new {@link EmailContentProcessor} instance.
   */
  public static EmailContentProcessor process(Message message) {
    return new EmailContentProcessor(message);
  }

  /**
   * @return the text body of the message.
   */
  public String getBody() {
    return body.toString().trim();
  }

  /**
   * @return an {@link ImmutableMap} with the attachments of an email message.
   */
  public List<MuleMessage> getAttachments() {
    return ImmutableList.copyOf(attachmentParts);
  }

  /**
   * Processes a {@link Multipart} which may contain INLINE content and ATTACHMENTS.
   *
   * @param part the part to be processed
   */
  private void processMultipartPart(Part part) {
    try {
      Multipart mp = (Multipart) part.getContent();
      for (int i = 0; i < mp.getCount(); i++) {
        processPart(mp.getBodyPart(i));
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException(ERROR_PROCESSING_MESSAGE, e);
    }
  }

  /**
   * Processes a single {@link Part} and adds it to the body of the message or as a new attachment depending on it's disposition
   * type.
   *
   * @param part the part to be processed
   */
  private void processPart(Part part) {
    try {
      Object content = part.getContent();
      if (isMultipart(content)) {
        processMultipartPart(part);
      }

      if (isAttachment(part)) {
        Map<String, LinkedList<String>> headers = new HashMap<>();
        final Enumeration allHeaders = part.getAllHeaders();
        while (allHeaders.hasMoreElements()) {
          Header h = (Header) allHeaders.nextElement();
          headers.put(h.getName(), new LinkedList<>(singletonList(h.getValue())));
        }

        attachmentParts.add(MuleMessage.builder().payload(part.getInputStream()).mediaType(MediaType.parse(part.getContentType()))
            .attributes(new PartAttributes(part.getFileName(), part.getFileName(), part.getSize(), headers)).build());
      } else {
        if (isText(content)) {
          body.add((String) content);
        }

        if (content instanceof InputStream && isInline(part) && part.isMimeType(TEXT)) {
          String inline = IOUtils.toString((InputStream) content);
          body.add(inline);
        }
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException(ERROR_PROCESSING_MESSAGE, e);
    }
  }

  /**
   * Evaluates whether the disposition of the {@link Part} is INLINE or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as inline, false otherwise
   * @throws MessagingException
   */
  private boolean isInline(Part part) throws MessagingException {
    return part.getDisposition().equalsIgnoreCase(Part.INLINE);
  }


  /**
   * Evaluates whether a {@link Part} is an attachment or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as an attachment, false otherwise
   * @throws MessagingException
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
