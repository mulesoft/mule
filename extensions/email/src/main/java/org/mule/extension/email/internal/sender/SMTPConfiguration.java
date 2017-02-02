/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Configuration for operations that are performed through the SMTP (Simple Mail Transfer Protocol) protocol.
 *
 * @since 4.0
 */
@Operations(SenderOperations.class)
@ConnectionProviders({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
@DisplayName("SMTP")
public class SMTPConfiguration {

  /**
   * The "From" sender address. The person that is going to send the messages.
   */
  @Parameter
  @Optional
  private String from;

  /**
   * Default character encoding to be used in all the messages. If not specified, the default charset in the mule configuration
   * will be used
   */
  @Parameter
  @DefaultEncoding
  private String defaultEncoding;

  /**
   * @return the address of the person that is going to send the messages.
   */
  public String getFrom() {
    return from;
  }

  public String getDefaultEncoding() {
    return defaultEncoding;
  }
}
