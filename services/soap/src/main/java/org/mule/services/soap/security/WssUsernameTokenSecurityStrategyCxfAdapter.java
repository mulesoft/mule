/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.services.soap.security;

import static java.util.Optional.of;
import static org.apache.ws.security.WSConstants.CREATED_LN;
import static org.apache.ws.security.WSConstants.NONCE_LN;
import static org.apache.ws.security.WSPasswordCallback.USERNAME_TOKEN;
import static org.apache.ws.security.handler.WSHandlerConstants.ADD_UT_ELEMENTS;
import static org.apache.ws.security.handler.WSHandlerConstants.PASSWORD_TYPE;
import static org.apache.ws.security.handler.WSHandlerConstants.USER;
import org.mule.services.soap.api.security.PasswordType;
import org.mule.services.soap.api.security.UsernameTokenSecurityStrategy;
import org.mule.services.soap.security.callback.WSPasswordCallbackHandler;

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
public class WssUsernameTokenSecurityStrategyCxfAdapter implements SecurityStrategyCxfAdapter {

  /**
   * The username required to authenticate with the service.
   */
  private String username;

  /**
   * The password for the provided username required to authenticate with the service.
   */
  private String password;

  /**
   * A {@link PasswordType} which qualifies the {@link #password} parameter.
   */
  private PasswordType passwordType;

  /**
   * Specifies a if a cryptographically random nonce should be added to the message.
   */
  private boolean addNonce;

  /**
   * Specifies if a timestamp should be created to indicate the creation time of the message.
   */
  private boolean addCreated;

  public WssUsernameTokenSecurityStrategyCxfAdapter(UsernameTokenSecurityStrategy usernameToken) {
    this.addCreated = usernameToken.isAddCreated();
    this.addNonce = usernameToken.isAddNonce();
    this.password = usernameToken.getPassword();
    this.username = usernameToken.getUsername();
    this.passwordType = usernameToken.getPasswordType();
  }

  public SecurityStrategyType securityType() {
    return SecurityStrategyType.OUTGOING;
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
