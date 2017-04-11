/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.extension.api.soap.security.TimestampSecurityStrategy;

import java.util.concurrent.TimeUnit;

/**
 * Bundles the outgoing SOAP message that it's being built with a timestamp that carries the creation.
 *
 * @since 4.0
 */
public class WssTimestampSecurityStrategy implements SecurityStrategyAdapter {

  /**
   * The time difference between creation and expiry time in seconds. After this time the message is invalid.
   */
  @Parameter
  @Optional(defaultValue = "60")
  private long timeToLive;

  /**
   * A {@link TimeUnit} which qualifies the {@link #timeToLive} parameter.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Summary("Time unit to be used in the timeToLive parameter")
  private TimeUnit timeToLiveUnit;

  @Override
  public SecurityStrategy getSecurityStrategy() {
    return new TimestampSecurityStrategy(timeToLiveUnit.toSeconds(timeToLive));
  }
}
