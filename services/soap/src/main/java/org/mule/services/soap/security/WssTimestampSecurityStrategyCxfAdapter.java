/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security;

import static java.util.Optional.empty;
import static org.apache.ws.security.handler.WSHandlerConstants.TIMESTAMP;
import static org.apache.ws.security.handler.WSHandlerConstants.TTL_TIMESTAMP;
import org.mule.services.soap.security.callback.WSPasswordCallbackHandler;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Bundles the outgoing SOAP message that it's being built with a timestamp that carries the creation.
 *
 * @since 4.0
 */
public class WssTimestampSecurityStrategyCxfAdapter implements SecurityStrategyCxfAdapter {

  /**
   * The time difference between creation and expiry time in seconds. After this time the message is invalid.
   */
  private long timeToLiveInSeconds;

  public WssTimestampSecurityStrategyCxfAdapter(long timeToLeaveInSeconds) {
    this.timeToLiveInSeconds = timeToLeaveInSeconds;
  }


  @Override
  public SecurityStrategyType securityType() {
    return SecurityStrategyType.OUTGOING;
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
    return ImmutableMap.<String, Object>builder().put(TTL_TIMESTAMP, String.valueOf(timeToLiveInSeconds)).build();
  }
}
