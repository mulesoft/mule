/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.services.soap.api.security;

/**
 * Provides the capability to authenticate using Username and Password with a SOAP service by adding the UsernameToken
 * element in the SOAP request.
 *
 * @since 4.0
 */
public final class UsernameTokenSecurityStrategy implements SecurityStrategy {

  /**
   * The username required to authenticate with the service.
   */
  private final String username;

  /**
   * The password for the provided username required to authenticate with the service.
   */
  private final String password;

  /**
   * A {@link PasswordType} which qualifies the {@link #password} parameter.
   */
  private final PasswordType passwordType;

  /**
   * Specifies a if a cryptographically random nonce should be added to the message.
   */
  private final boolean addNonce;

  /**
   * Specifies if a timestamp should be created to indicate the creation time of the message.
   */
  private final boolean addCreated;

  public UsernameTokenSecurityStrategy(String username,
                                       String password,
                                       PasswordType passwordType,
                                       boolean addNonce,
                                       boolean addCreated) {
    this.username = username;
    this.password = password;
    this.passwordType = passwordType;
    this.addNonce = addNonce;
    this.addCreated = addCreated;
  }

  public UsernameTokenSecurityStrategy(String username,
                                       String password,
                                       PasswordType passwordType) {
    this.username = username;
    this.password = password;
    this.passwordType = passwordType;
    this.addCreated = false;
    this.addNonce = false;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public PasswordType getPasswordType() {
    return passwordType;
  }

  public boolean isAddNonce() {
    return addNonce;
  }

  public boolean isAddCreated() {
    return addCreated;
  }

  @Override
  public void accept(SecurityStrategyVisitor visitor) {
    visitor.visitUsernameToken(this);
  }
}
