/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;


import static org.mule.extension.email.internal.util.EmailConnectorUtils.TEXT_PLAIN;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;

/**
 * Represents and enables the construction of the content of an email with a body of type "text/*" and a specific character
 * encoding.
 *
 * @since 4.0
 */
public class EmailContent {

  /**
   * Text body of the message content
   */
  @Parameter
  @Optional(defaultValue = "#[payload]")
  @Placement(order = 1)
  @Text
  private String body;
  /**
   * ContentType of the body text. Example: "text/html" or "text/plain".
   */
  @Parameter
  @Optional(defaultValue = TEXT_PLAIN)
  @DisplayName("ContentType")
  @Placement(order = 2)
  private String contentType;
  /**
   * The character encoding of the body. If not specified, it defaults to the default charset in the mule configuration
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String charset;

  public EmailContent() {}

  public EmailContent(String body, String charset) {
    this.body = body;
    this.contentType = TEXT_PLAIN;
    this.charset = charset;
  }

  public EmailContent(String body, MediaType contentType, String charset) {
    this.body = body;
    this.contentType = contentType.toString();
    this.charset = charset;
  }

  /**
   * @return the body of the message content. The body aims to be text.
   */
  public String getBody() {
    return body;
  }

  /**
   * @return the contentType of the body. one of "text/html" or "text/plain"
   */
  public MediaType getContentType() {
    return DataType.builder().mediaType(contentType).build().getMediaType();
  }

  /**
   * @return the charset of the body.
   */
  public String getCharset() {
    return charset;
  }
}
