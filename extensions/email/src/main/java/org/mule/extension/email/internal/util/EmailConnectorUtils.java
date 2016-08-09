/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * this class contains common methods for email handling.
 *
 * @since 4.0
 */
public final class EmailConnectorUtils {

  /**
   * Default folder name for all the mailboxes.
   */
  public static final String INBOX_FOLDER = "INBOX";

  /**
   * defines all the multipart content types
   */
  public static final String MULTIPART = "multipart/*";

  /**
   * defines all the text content types
   */
  public static final String TEXT = "text/*";

  /**
   * Default email content type
   */
  public static final String TEXT_PLAIN = "text/plain";

  /**
   * Default port value for SMTP servers.
   */
  public static final String SMTP_PORT = "25";

  /**
   * Default port value for SMTPS servers.
   */
  public static final String SMTPS_PORT = "465";

  /**
   * Default port value for POP3 servers.
   */
  public static final String POP3_PORT = "110";

  /**
   * Default port value for POP3S servers.
   */
  public static final String POP3S_PORT = "995";

  /**
   * Default port value for IMAP servers.
   */
  public static final String IMAP_PORT = "143";

  /**
   * Default port value for IMAPS servers.
   */
  public static final String IMAPS_PORT = "993";

  /**
   * Hide constructor
   */
  private EmailConnectorUtils() {}

  /**
   * Converts a simple {@link String} representing an address into an {@link InternetAddress} instance
   *
   * @param address the string to be converted.
   * @return a new {@link InternetAddress} instance.
   */
  public static Address toAddress(String address) {
    try {
      return new InternetAddress(address);
    } catch (AddressException e) {
      throw new EmailException(format("Error while creating %s InternetAddress", address));
    }
  }

  /**
   * Converts a {@link List} of {@link String}s representing email addresses into an {@link InternetAddress} array.
   *
   * @param addresses the list to be converted.
   * @return a new {@link Address}[] instance.
   */
  public static Address[] toAddressArray(List<String> addresses) {
    return addresses.stream().map(EmailConnectorUtils::toAddress).toArray(Address[]::new);
  }

  /**
   * Extracts the incoming {@link MuleMessage} attributes of {@link EmailAttributes} type.
   *
   * @param muleMessage the incoming {@link MuleMessage}.
   * @return an {@link Optional} value with the {@link EmailAttributes}.
   */
  public static Optional<EmailAttributes> getAttributesFromMessage(MuleMessage muleMessage) {
    if (muleMessage.getAttributes() instanceof EmailAttributes) {
      return Optional.ofNullable((EmailAttributes) muleMessage.getAttributes());
    }
    return Optional.empty();
  }

  /**
   * Transforms the attachments in a {@link MultiPartPayload} into a {@link List} of {@link EmailAttachment}s.
   *
   * @param payload a {@link MultiPartPayload} that carries the attachments.
   * @return a {@link List} of {@link EmailAttachment}, or an empty {@link List} if payload is not a {@link MultiPartPayload}.
   */
  public static List<EmailAttachment> mapToEmailAttachments(Object payload) {
    return payload instanceof DefaultMultiPartPayload
        ? ((DefaultMultiPartPayload) payload)
            .getNonBodyParts().stream().map(p -> new EmailAttachment(((PartAttributes) p.getAttributes()).getName(),
                                                                     p.getPayload(), p.getDataType().getMediaType()))
            .collect(toList())
        : Collections.emptyList();
  }
}
