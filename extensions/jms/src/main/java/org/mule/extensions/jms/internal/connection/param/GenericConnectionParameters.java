/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.param;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.internal.connection.provider.BaseConnectionProvider;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

/**
 * Common connection parameters for the {@link BaseConnectionProvider}
 *
 * @since 4.0
 */
public class GenericConnectionParameters {

  /**
   * Username to be used when providing credentials fpr authentication.
   */
  @Parameter
  @Optional
  private String username;

  /**
   * Password to be used when providing credentials fpr authentication.
   */
  @Parameter
  @Optional
  @Password
  private String password;

  /**
   *  Client identifier to be assigned to the {@link Connection} upon creation.
   *  The purpose of client identifier is to associate a connection and its objects
   *  with a state maintained on behalf of the client by a provider. By definition,
   *  the client state identified by a client identifier can be "in use" by only one
   *  client at a time.
   *  <p>
   *  The only use of a client identifier defined by JMS is its mandatory use in
   *  identifying an unshared durable subscription or its optional use in identifying
   *  a shared durable or non-durable subscription.
   */
  @Parameter
  @Optional
  private String clientId;

  /**
   * Versions of the {@link JmsSpecification} to be used by the extension.
   * This version should be compatible with the implementation of the {@link ConnectionFactory}
   * configured. Functionality available only for certain versions of the spec
   * will throw an error if the version requirement is not met.
   */
  @Parameter
  @Optional(defaultValue = "JMS_1_1")
  @Expression(NOT_SUPPORTED)
  private JmsSpecification specification;

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getClientId() {
    return clientId;
  }

  public JmsSpecification getSpecification() {
    return specification;
  }

}
