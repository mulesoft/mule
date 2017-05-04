/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;


import static org.mule.extension.email.internal.util.EmailConnectorConstants.TEXT_PLAIN;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

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
  @Content(primary = true)
  @Placement(order = 1)
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
   * The character encoding of the body. If not specified, it defaults to the default encoding in the mule configuration
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String encoding;

  public EmailBody() {}

  public EmailBody(String content, MediaType contentType, String encoding) {
    this.content = content;
    this.contentType = contentType.toString();
    this.encoding = encoding;
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
   * @return the encoding of the body.
   */
  public String getEncoding() {
    return encoding;
  }
}
