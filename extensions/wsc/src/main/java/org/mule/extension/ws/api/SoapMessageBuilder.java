/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import static java.util.Collections.emptyMap;
import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

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
  private String body;

  /**
   * The XML headers to include in the SOAP message.
   */
  // TODO remove empty map MULE-10901
  @Parameter
  @Optional
  @NullSafe
  private Map<String, String> headers = emptyMap();

  /**
   * The attachments to include in the SOAP request.
   */
  // TODO remove empty map MULE-10901
  @Parameter
  @Optional
  @NullSafe
  private Map<String, SoapAttachment> attachments = emptyMap();


  public String getBody() {
    return body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Map<String, SoapAttachment> getAttachments() {
    return attachments;
  }
}
