/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.GENERAL;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.inject.Inject;

/**
 * Configuration for operations that are performed through the SMTP (Simple Mail Transfer Protocol) protocol.
 *
 * @since 4.0
 */
@Operations(SenderOperations.class)
@ConnectionProviders({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
@DisplayName("SMTP")
public class SMTPConfiguration implements Initialisable {

  @Inject
  private MuleContext muleContext;

  /**
   * The "From" sender address. The person that is going to send the messages.
   */
  @Parameter
  @Optional
  @Placement(group = GENERAL)
  private String from;

  /**
   * Default character encoding to be used in all the messages. If not specified, the default charset in the mule configuration
   * will be used
   */
  @Parameter
  @Optional
  @Placement(group = ADVANCED)
  private String defaultCharset;

  /**
   * A global set of headers that is bounded in each SMTP operation.
   */
  @Parameter
  @Optional
  @Placement(group = ADVANCED)
  private Map<String, String> headers;

  /**
   * @return the address of the person that is going to send the messages.
   */
  public String getFrom() {
    return from;
  }

  public String getDefaultCharset() {
    return defaultCharset;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (defaultCharset == null) {
      defaultCharset = muleContext.getConfiguration().getDefaultEncoding();
    }
  }

  public Map<String, String> getHeaders() {
    return headers != null ? ImmutableMap.copyOf(headers) : emptyMap();
  }
}
