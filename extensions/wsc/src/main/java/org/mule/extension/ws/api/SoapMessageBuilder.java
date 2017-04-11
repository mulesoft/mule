/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapAttachment;

import java.util.Map;

/**
 * Component that specifies how to create a proper SOAP request using the {@link WebServiceConsumer}.
 *
 * @since 4.0
 */
public class SoapMessageBuilder {

  /**
   * The XML body to include in the SOAP message, with all the required parameters, or {@code null} if no params are required.
   */
  @Parameter
  @Optional
  @Content(primary = true)
  private String body;

  /**
   * The XML headers to include in the SOAP message.
   */
  @Parameter
  @Optional
  @Content
  private String headers;

  /**
   * The attachments to include in the SOAP request.
   */
  @Parameter
  @Optional
  @NullSafe
  @Content
  private Map<String, SoapAttachment> attachments;

  public String getBody() {
    return body;
  }

  public String getHeaders() {
    return headers;
  }

  public Map<String, SoapAttachment> getAttachments() {
    return attachments;
  }
}
