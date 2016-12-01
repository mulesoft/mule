/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;

import static java.lang.Long.valueOf;
import static java.util.Optional.empty;
import static org.apache.ws.security.handler.WSHandlerConstants.TIMESTAMP;
import static org.apache.ws.security.handler.WSHandlerConstants.TTL_TIMESTAMP;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Bundles the outgoing SOAP message that it's being built with a timestamp that carries the creation.
 *
 * @since 4.0
 */
public class WssTimestampSecurityStrategy implements SecurityStrategy {

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
  public SecurityStrategyType securityType() {
    return OUTGOING;
  }

  @Override
  public java.util.Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return empty();
  }

  @Override
  public String securityAction() {
    return TIMESTAMP;
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    return ImmutableMap.<String, Object>builder().put(TTL_TIMESTAMP, valueOf(timeToLiveUnit.toSeconds(timeToLive))).build();
  }
}
