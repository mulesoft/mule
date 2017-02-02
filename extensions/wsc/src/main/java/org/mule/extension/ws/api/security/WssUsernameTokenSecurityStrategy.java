/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.ws.api.security;

import static java.util.Optional.of;
import static org.apache.ws.security.WSConstants.CREATED_LN;
import static org.apache.ws.security.WSConstants.NONCE_LN;
import static org.apache.ws.security.WSPasswordCallback.USERNAME_TOKEN;
import static org.apache.ws.security.handler.WSHandlerConstants.ADD_UT_ELEMENTS;
import static org.apache.ws.security.handler.WSHandlerConstants.PASSWORD_TYPE;
import static org.apache.ws.security.handler.WSHandlerConstants.USER;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.api.PasswordType;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.StringJoiner;

import org.apache.ws.security.handler.WSHandlerConstants;

/**
 * Provides the capability to authenticate using Username and Password with a SOAP service by adding the UsernameToken
 * element in the SOAP request.
 *
 * @since 4.0
 */
public class WssUsernameTokenSecurityStrategy implements SecurityStrategy {

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
  public SecurityStrategyType securityType() {
    return OUTGOING;
  }

  @Override
  public java.util.Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return of(new WSPasswordCallbackHandler(USERNAME_TOKEN,
                                            cb -> {
                                              if (cb.getIdentifier().equals(username)) {
                                                cb.setPassword(password);
                                              }
                                            }));
  }

  @Override
  public String securityAction() {
    return WSHandlerConstants.USERNAME_TOKEN;
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    builder.put(USER, username);
    builder.put(PASSWORD_TYPE, passwordType.getType());

    if (addCreated || addNonce) {
      StringJoiner additionalElements = new StringJoiner(" ");
      if (addNonce) {
        additionalElements.add(NONCE_LN);
      }
      if (addCreated) {
        additionalElements.add(CREATED_LN);
      }
      builder.put(ADD_UT_ELEMENTS, additionalElements.toString());
    }

    return builder.build();
  }
}
