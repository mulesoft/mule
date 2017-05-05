/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;
import java.util.Map;

/**
 * Enables the creation of an outgoing email. Users must use this builder
 * to create a email instance from xml to be sent over SMTP.
 *
 * @since 4.0
 */
public class EmailSettings {

  /**
   * The "From" sender address. The person that is going to send the messages,
   * if not set, it defaults to the from address specified in the config.
   */
  @Optional
  @Parameter
  @Summary("The sender 'From' address. If not provided, it defaults to the address specified in the config")
  @Example("example@company.com")
  private String fromAddress;

  /**
   * The recipient addresses of "To" (primary) type.
   */
  @Parameter
  private List<String> toAddresses;

  /**
   * The recipient addresses of "Cc" (carbon copy) type
   */
  @Optional
  @Parameter
  @NullSafe
  @Summary("The recipient addresses of 'Cc' (carbon copy) type")
  private List<String> ccAddresses;

  /**
   * The recipient addresses of "Bcc" (blind carbon copy) type
   */
  @Optional
  @Parameter
  @NullSafe
  @Summary("The recipient addresses of 'Bcc' (blind carbon copy) type")
  private List<String> bccAddresses;

  /**
   * The email addresses to which this email should be replied.
   */
  @Optional
  @Parameter
  @NullSafe
  @Summary("The email addresses to which this email should be replied")
  private List<String> replyToAddresses;

  /**
   * The subject of the email.
   */
  @Optional(defaultValue = "[No Subject]")
  @Parameter
  @Summary("The subject of the email")
  private String subject;

  /**
   * The headers that this email carry.
   */
  @Optional
  @Parameter
  @Content
  @NullSafe
  @Summary("The custom headers that this email will carry")
  @Example("#[{'X-MC-Autotext': 'yes'}]")
  @Placement(tab = ADVANCED_TAB)
  private Map<String, String> headers;


  /**
   * Creates a new instance.
   */
  public EmailSettings() {}

  /**
   * @return the configured subject for the built outgoing email.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return a {@link List} with the addresses that this mail should be replied to configured in the built outgoing email.
   */
  public List<String> getReplyToAddresses() {
    return replyToAddresses;
  }

  /**
   * @return a {@link List} with the primary recipients configured in the built outgoing email.
   */
  public List<String> getToAddresses() {
    return toAddresses;
  }

  /**
   * @return a {@link List} with the cc recipients configured in the built outgoing email.
   */
  public List<String> getCcAddresses() {
    return ccAddresses;
  }

  /**
   * @return a {@link List} with the bcc recipients configured in the built outgoing email.
   */
  public List<String> getBccAddresses() {
    return bccAddresses;
  }

  /**
   * @return a {@link Map} with all the additional headers configured for the
   */
  public Map<String, String> getHeaders() {
    return headers != null ? headers : emptyMap();
  }

  /**
   * @return the from address, the address of the sender of the email.
   */
  public String getFromAddress() {
    return fromAddress;
  }

}
