/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import org.mule.runtime.extension.api.soap.SoapAttachment;

import java.io.InputStream;
import java.util.Map;

/**
 * Represents a Soap Message carrying a content, attachments and a set of headers.
 *
 * @since 4.0
 */
public interface SoapMessage extends WithContentType {

  /**
   * @return the content of the message.
   */
  InputStream getContent();

  /**
   * @return a set of Soap Headers.
   */
  Map<String, String> getSoapHeaders();

  /**
   * @return a set of Transport Specific Headers.
   */
  Map<String, String> getTransportHeaders();

  /**
   * @return a set of attachments.
   */
  Map<String, SoapAttachment> getAttachments();
}
