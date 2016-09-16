/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;


import static org.mule.extension.email.internal.util.EmailConnectorConstants.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;

/**
 * Represents and enables the construction of the body of an email with a body of type "text/*" and a specific character
 * encoding.
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
public class EmailBody {

  /**
   * Text body of the message content
   */
  @Parameter
  @Optional(defaultValue = PAYLOAD)
  @Placement(order = 1)
  @Text
  private String content;

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

  public EmailBody() {}

  public EmailBody(String content, MediaType contentType, String charset) {
    this.content = content;
    this.contentType = contentType.toString();
    this.charset = charset;
  }

  /**
   * @return the body of the message content. The body aims to be text.
   */
  public String getContent() {
    return content;
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
