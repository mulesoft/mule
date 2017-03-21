/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import org.mule.runtime.api.metadata.MediaType;

import java.io.InputStream;

/**
 * Represents and enables the construction of an attachment to be sent over SOAP.
 *
 * @since 4.0
 */
public class WscSoapAttachment {

  /**
   * the content of the attachment.
   */
  private InputStream content;

  /**
   * the content type of the attachment content.
   */
  private MediaType contentType;

  public WscSoapAttachment() {}

  /**
   * @return the content of the attachment.
   */
  public InputStream getContent() {
    return content;
  }

  /**
   * @return the content type of the attachment content.
   */
  public MediaType getContentType() {
    return contentType;
  }
}
