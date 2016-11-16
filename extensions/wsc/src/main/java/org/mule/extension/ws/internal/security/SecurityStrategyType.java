/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security;

import org.mule.extension.ws.api.security.SecurityStrategy;

/**
 * Different types of {@link SecurityStrategy} that specify when a strategy should be applied to a message.
 *
 * @since 4.0
 */
public enum SecurityStrategyType {

  /**
   * For configurations that should be applied to incoming (response) messages.
   * This configuration type is used for decrypting and verifying the signature of incoming messages.
   */
  INCOMING,

  /**
   * For configurations that should be applied to outgoing (request) messages.
   * This configuration type is used for encryption, signing and adding SAML, timestamp and username headers.
   */
  OUTGOING
}
