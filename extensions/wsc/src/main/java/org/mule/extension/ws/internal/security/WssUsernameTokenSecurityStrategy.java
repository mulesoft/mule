/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.ws.internal.security;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.soap.security.PasswordType;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.extension.api.soap.security.UsernameTokenSecurityStrategy;

/**
 * Provides the capability to authenticate using Username and Password with a SOAP service by adding the UsernameToken
 * element in the SOAP request.
 *
 * @since 4.0
 */
public class WssUsernameTokenSecurityStrategy implements SecurityStrategyAdapter {

  /**
   * The username required to authenticate with the service.
   */
  @Parameter
  private String username;

  /**
   * The password for the provided username required to authenticate with the service.
   */
  @Parameter
  @Password
  private String password;

  /**
   * A {@link PasswordType} which qualifies the {@link #password} parameter.
   */
  @Parameter
  @Optional(defaultValue = "TEXT")
  @Summary("The type of the password that is provided. One of Digest or Text")
  private PasswordType passwordType;

  /**
   * Specifies a if a cryptographically random nonce should be added to the message.
   */
  @Parameter
  @Optional
  private boolean addNonce;

  /**
   * Specifies if a timestamp should be created to indicate the creation time of the message.
   */
  @Parameter
  @Optional
  private boolean addCreated;

  @Override
  public SecurityStrategy getSecurityStrategy() {
    return new UsernameTokenSecurityStrategy(username, password, passwordType, addNonce, addCreated);
  }
}
