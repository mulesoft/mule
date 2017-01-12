/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

/**
 * Represents and enables the construction of an attachment to be sent over SOAP.
 *
 * @since 4.0
 */
public class SoapAttachment {

  /**
   * the name of the attachment.
   */
  private String id;

  /**
   * the content of the attachment.
   */
  private Object content;

  /**
   * the content type of the attachment content.
   */
  private String contentType;

  public SoapAttachment() {}

  /**
   * @return the content id of the attachment.
   */
  public String getId() {
    return id;
  }

  /**
   * @return the content of the attachment.
   */
  public Object getContent() {
    return content;
  }

  /**
   * @return the content type of the attachment content.
   */
  public MediaType getContentType() {
    return contentType != null ? DataType.builder().mediaType(contentType).build().getMediaType() : null;
  }
}
