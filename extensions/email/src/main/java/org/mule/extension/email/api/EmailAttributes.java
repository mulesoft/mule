/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.api.message.Attributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Base representation of an email's metadata attributes.
 * <p>
 * It contains information such as a file's email id, subject, to addresses, etc.
 *
 * @since 4.0
 */
public interface EmailAttributes extends Attributes {

  /**
   * Get the Message id of the email.
   *
   * @return the message id
   */
  int getId();

  /**
   * Get the addresses to which replies should be directed. This will usually be the sender of the email, but some emails may
   * direct replies to a different address
   *
   * @return all the recipient addresses of replyTo type.
   */
  List<String> getReplyToAddresses();

  /**
   * @return the subject of the email.
   */
  String getSubject();

  /**
   * @return all the recipient addresses of "To" (primary) type.
   */
  List<String> getToAddresses();

  /**
   * @return all the recipient addresses of "Bcc" (blind carbon copy) type.
   */
  List<String> getBccAddresses();

  /**
   * @return all the recipient addresses of "Cc" (carbon copy) type.
   */
  List<String> getCcAddresses();

  /**
   * Get the identity of the person(s) who wished this message to be sent.
   *
   * @return all the from addresses.
   */
  List<String> getFromAddresses();

  /**
   * Get the date this message was received.
   *
   * @return the date this message was received.
   */
  LocalDateTime getReceivedDate();

  /**
   * Get the date this message was sent.
   *
   * @return the date this message was sent.
   */
  LocalDateTime getSentDate();

  /**
   * @return all the headers of this email message.
   */
  Map<String, String> getHeaders();
}
